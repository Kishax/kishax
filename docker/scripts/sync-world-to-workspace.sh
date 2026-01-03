#!/bin/bash
# ================================================================
# S3 Workspace Upload Script
# ================================================================
# Sync Minecraft world data to S3 workspace (uncompressed)
#
# Usage: ./sync-world-to-workspace.sh [OPTIONS]
# Location: EC2 i-a instance, inside Docker container (/mc/scripts/)
# Returns: 0=success, 1=error
# ================================================================

set -e

# ================================================================
# Configuration
# ================================================================

CONFIG_FILE="/mc/config/servers.json"
S3_BUCKET="${S3_BUCKET:-kishax-production-world-backups}"
S3_WORKSPACE_PREFIX="${S3_WORKSPACE_PREFIX:-workspace/}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
DRY_RUN=false
TARGET_SERVER=""

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ================================================================
# Helper Functions
# ================================================================

print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ================================================================
# Options Parser
# ================================================================

usage() {
    cat << EOF
S3 Workspace Upload Script

Usage: $0 [OPTIONS]

Options:
  --dry-run                実際にはアップロードせず、何が実行されるか確認
  --server <name>          特定サーバーのみ同期
  --help                   このヘルプを表示

Examples:
  $0                                    # 全サーバーを同期
  $0 --dry-run                          # ドライラン
  $0 --server home                      # homeサーバーのみ同期

Environment Variables:
  S3_BUCKET              S3バケット名 (デフォルト: kishax-production-world-backups)
  S3_WORKSPACE_PREFIX    S3プレフィックス (デフォルト: workspace/)
  AWS_REGION             AWSリージョン (デフォルト: ap-northeast-1)

Note: workspace/ は非圧縮で差分のみ転送します（高速）
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --server)
            TARGET_SERVER="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            print_error "不明なオプション: $1"
            echo "ヘルプを表示: $0 --help"
            exit 1
            ;;
    esac
done

# ================================================================
# Prerequisites Check
# ================================================================

check_prerequisites() {
    print_header "前提条件チェック"

    # servers.json existence
    if [ ! -f "$CONFIG_FILE" ]; then
        print_error "設定ファイルが見つかりません: $CONFIG_FILE"
        exit 1
    fi
    print_success "設定ファイル確認: $CONFIG_FILE"

    # jq command
    if ! command -v jq &> /dev/null; then
        print_error "jq コマンドが見つかりません"
        exit 1
    fi
    print_success "jq インストール済み"

    # AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI が見つかりません"
        exit 1
    fi
    print_success "AWS CLI インストール済み"

    # S3 bucket access check
    if [ "$DRY_RUN" = false ]; then
        if ! aws s3 ls "s3://$S3_BUCKET" --region "$AWS_REGION" &> /dev/null; then
            print_error "S3バケットにアクセスできません: s3://$S3_BUCKET"
            print_info "IAMロール権限を確認してください"
            exit 1
        fi
        print_success "S3バケットアクセス確認"
    else
        print_info "ドライランモード: S3アクセスチェックをスキップ"
    fi

    echo ""
}

# ================================================================
# Get Active Servers
# ================================================================

get_active_servers() {
    local servers=()
    local spigot_count=$(jq -r '.spigots | length' "$CONFIG_FILE")

    for ((i=0; i<spigot_count; i++)); do
        local name=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
        local memory_ratio=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")

        # Skip disabled servers (memory_ratio = 0)
        if (( $(echo "$memory_ratio == 0" | bc -l) )); then
            continue
        fi

        # If target server specified, only include that server
        if [ -n "$TARGET_SERVER" ] && [ "$name" != "$TARGET_SERVER" ]; then
            continue
        fi

        servers+=("$name")
    done

    echo "${servers[@]}"
}

# ================================================================
# Sync Single Server to Workspace
# ================================================================

sync_server() {
    local server_name=$1
    local server_dir="/mc/spigot/$server_name"
    local s3_workspace_path="$S3_WORKSPACE_PREFIX$server_name"

    print_header "同期: $server_name"

    # Check if server directory exists
    if [ ! -d "$server_dir" ]; then
        print_warning "サーバーディレクトリが見つかりません: $server_dir"
        return 1
    fi

    # Dynamically detect all world directories (world*)
    local world_types=()
    while IFS= read -r -d '' world_dir; do
        world_types+=("$(basename "$world_dir")")
    done < <(find "$server_dir" -maxdepth 1 -type d -name "world*" -print0 | sort -z)

    if [ ${#world_types[@]} -eq 0 ]; then
        print_warning "ワールドディレクトリが見つかりません: $server_dir"
        return 1
    fi

    print_info "  検出されたワールド: ${world_types[*]}"

    local sync_count=0
    local total_size=0

    for world_type in "${world_types[@]}"; do
        local world_path="$server_dir/$world_type"

        if [ ! -d "$world_path" ]; then
            print_info "  ⏭️  $world_type: ディレクトリが見つかりません（スキップ）"
            continue
        fi

        print_info "  📤 $world_type: 同期中..."

        # Calculate world size
        local world_size=$(du -sh "$world_path" 2>/dev/null | cut -f1)
        print_info "     サイズ: $world_size"

        # Sync to S3 (uncompressed, diff only)
        if [ "$DRY_RUN" = false ]; then
            if aws s3 sync "$world_path/" "s3://$S3_BUCKET/$s3_workspace_path/$world_type/" \
                --region "$AWS_REGION" \
                --delete \
                --no-progress; then
                print_success "     同期完了"
                sync_count=$((sync_count + 1))

                # Calculate total size
                local world_bytes=$(du -sb "$world_path" | cut -f1)
                total_size=$((total_size + world_bytes))
            else
                print_error "     同期失敗: $world_type"
                return 1
            fi
        else
            print_info "     (dryrun) 同期をスキップ"
            sync_count=$((sync_count + 1))
        fi
    done

    if [ $sync_count -eq 0 ]; then
        print_warning "同期対象が見つかりませんでした: $server_name"
        return 1
    fi

    # Create metadata file
    print_info "  📝 メタデータ作成中..."

    if [ "$DRY_RUN" = false ]; then
        local metadata_file="/tmp/metadata-$server_name-$$.json"
        cat > "$metadata_file" << EOF
{
  "server": "$server_name",
  "timestamp": "$TIMESTAMP",
  "type": "workspace",
  "compressed": false,
  "total_size_bytes": $total_size,
  "worlds": [
EOF

        local first=true
        for world_type in "${world_types[@]}"; do
            local world_path="$server_dir/$world_type"
            if [ -d "$world_path" ]; then
                if [ "$first" = false ]; then
                    echo "," >> "$metadata_file"
                fi
                first=false

                local size=$(du -sb "$world_path" | cut -f1)
                cat >> "$metadata_file" << EOF
    {
      "world": "$world_type",
      "size_bytes": $size
    }
EOF
            fi
        done

        cat >> "$metadata_file" << EOF

  ]
}
EOF

        aws s3 cp "$metadata_file" "s3://$S3_BUCKET/$s3_workspace_path/metadata.json" \
            --region "$AWS_REGION"
        rm -f "$metadata_file"

        print_success "  メタデータ作成完了"
    else
        print_info "  (dryrun) メタデータ作成をスキップ"
    fi

    echo ""
    return 0
}

# ================================================================
# Main
# ================================================================

main() {
    clear

    print_header "S3 Workspace Upload Script"
    echo "📍 S3保存先: s3://$S3_BUCKET/$S3_WORKSPACE_PREFIX"
    echo "🔧 AWS リージョン: $AWS_REGION"

    if [ -n "$TARGET_SERVER" ]; then
        echo "🎯 対象サーバー: $TARGET_SERVER"
    fi

    if [ "$DRY_RUN" = true ]; then
        print_warning "🧪 ドライランモード（実際にはアップロードしません）"
    fi

    echo ""

    # Prerequisites check
    check_prerequisites

    # Get active servers
    print_header "対象サーバー取得"
    local servers=($(get_active_servers))

    if [ ${#servers[@]} -eq 0 ]; then
        if [ -n "$TARGET_SERVER" ]; then
            print_error "指定されたサーバーが見つかりません: $TARGET_SERVER"
        else
            print_error "同期対象サーバーが見つかりません"
        fi
        exit 1
    fi

    echo "📋 対象サーバー数: ${#servers[@]}"
    for server in "${servers[@]}"; do
        echo "  - $server"
    done
    echo ""

    # Confirmation
    if [ "$DRY_RUN" = false ]; then
        print_warning "⚠️  Workspace用データとしてS3にアップロードします（非圧縮・差分のみ）"
        echo ""
        read -p "同期を開始しますか？ (y/N): " answer
        if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
            print_info "キャンセルしました"
            exit 0
        fi
        echo ""
    fi

    # Sync each server
    local success_count=0
    local fail_count=0

    for server in "${servers[@]}"; do
        if sync_server "$server"; then
            success_count=$((success_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi
    done

    # Summary
    echo ""
    print_header "同期結果"
    echo "✅ 成功: $success_count"
    echo "❌ 失敗: $fail_count"
    echo ""

    if [ "$DRY_RUN" = false ] && [ $success_count -gt 0 ]; then
        print_success "Workspaceへの同期が完了しました！"
        echo ""
        print_info "次のステップ:"
        print_info "1. S3の内容を確認:"
        print_info "   make workspace-list"
        print_info ""
        print_info "2. Workspaceからダウンロード:"
        print_info "   make workspace-download"
        print_info ""
        print_info "3. Workspaceをデプロイメントに変換:"
        print_info "   make workspace-to-deployment"
    elif [ "$DRY_RUN" = true ]; then
        print_info "ドライランが完了しました"
        print_info "実際に同期を実行するには、--dry-run オプションを外してください"
    fi

    echo ""

    if [ $fail_count -gt 0 ]; then
        exit 1
    fi

    exit 0
}

# Execute main
main
