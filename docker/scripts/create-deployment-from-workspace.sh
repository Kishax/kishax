#!/bin/bash
# ================================================================
# Create Deployment from Workspace Script
# ================================================================
# Download from S3 workspace (uncompressed) and deploy to deployment/ (compressed)
#
# Usage: ./create-deployment-from-workspace.sh [OPTIONS]
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
S3_DEPLOY_PREFIX="${S3_DEPLOY_PREFIX:-deployment/}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
YEAR_MONTH=$(date +%Y%m)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
VERSION="1"
VERSION_MANUALLY_SET=false
DRY_RUN=false
TARGET_SERVER=""
COMPRESSION_LEVEL=6
TEMP_DIR="/tmp/mc-workspace-to-deployment-$$"

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
# Versioning Logic
# ================================================================

# 全ての年月ディレクトリを走査して、最新のバージョン番号を取得する
get_next_version() {
    local root_prefix="s3://$S3_BUCKET/$S3_DEPLOY_PREFIX"

    # 1. まず全ての年月ディレクトリ(YYYYMM/)を取得
    local months
    months=$(aws s3 ls "$root_prefix" --region "$AWS_REGION" 2>/dev/null | \
             grep 'PRE ' | \
             awk '{print $2}' | \
             sed 's/\///g' | \
             grep '^[0-9]\{6\}$' | \
             sort -rn)

    if [ -z "$months" ]; then
        echo "1"
        return
    fi

    # 2. 最新の（一番数字が大きい）月のディレクトリの中身を確認
    # ただし、最新の月の中にバージョンがない可能性も考慮し、見つかるまでループ
    for month in $months; do
        local prefix="$root_prefix$month/"
        local latest_in_month
        latest_in_month=$(aws s3 ls "$prefix" --region "$AWS_REGION" 2>/dev/null | \
                         grep 'PRE ' | \
                         awk '{print $2}' | \
                         sed 's/\///g' | \
                         grep '^[0-9]\+$' | \
                         sort -rn | \
                         head -n 1 || true)

        if [ -n "$latest_in_month" ]; then
            echo $((latest_in_month + 1))
            return
        fi
    done

    echo "1"
}

# ================================================================
# Options Parser
# ================================================================

usage() {
    cat << EOF
Create Deployment from Workspace Script

Usage: $0 [OPTIONS]

Options:
  --dry-run                実際にはアップロードせず、何が実行されるか確認
  --server <name>          特定サーバーのみ変換
  --version <num>          バージョン番号 (指定しない場合は自動的に次の番号を採番)
  --compression <1-9>      圧縮レベル (1=速い/大きい, 9=遅い/小さい, デフォルト: 6)
  --help                   このヘルプを表示

Examples:
  $0                                    # 全サーバーを変換 (自動採番)
  $0 --dry-run                          # ドライラン
  $0 --server home                      # homeサーバーのみ変換
  $0 --version 5                        # 強制的にバージョン5として保存
  $0 --compression 9                    # 最大圧縮

Environment Variables:
  S3_BUCKET              S3バケット名 (デフォルト: kishax-production-world-backups)
  S3_WORKSPACE_PREFIX    S3 Workspaceプレフィックス (デフォルト: workspace/)
  S3_DEPLOY_PREFIX       S3デプロイプレフィックス (デフォルト: deployment/)
  AWS_REGION             AWSリージョン (デフォルト: ap-northeast-1)

Note: Workspaceから非圧縮データをダウンロード → 圧縮 → Deploymentへアップロード
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
        --version)
            VERSION="$2"
            VERSION_MANUALLY_SET=true
            if [[ ! "$VERSION" =~ ^[0-9]+$ ]]; then
                print_error "バージョンは正の整数で指定してください"
                exit 1
            fi
            shift 2
            ;;
        --compression)
            COMPRESSION_LEVEL="$2"
            if [[ ! "$COMPRESSION_LEVEL" =~ ^[1-9]$ ]]; then
                print_error "圧縮レベルは1-9の数値で指定してください"
                exit 1
            fi
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
# Get Available Workspace Servers
# ================================================================

get_workspace_servers() {
    local servers=()

    # Get workspace servers from S3
    while IFS= read -r line; do
        if [[ $line == *"PRE"* ]]; then
            local server_name=$(echo "$line" | awk '{print $2}' | sed 's/\///g')
            # If target server specified, only include that server
            if [ -n "$TARGET_SERVER" ] && [ "$server_name" != "$TARGET_SERVER" ]; then
                continue
            fi
            servers+=("$server_name")
        fi
    done < <(aws s3 ls "s3://$S3_BUCKET/$S3_WORKSPACE_PREFIX" --region "$AWS_REGION" 2>/dev/null)

    echo "${servers[@]}"
}

# ================================================================
# Process Single Server
# ================================================================

process_server() {
    local server_name=$1
    local s3_workspace_path="$S3_WORKSPACE_PREFIX$server_name"
    local s3_deploy_path="$S3_DEPLOY_PREFIX$YEAR_MONTH/$VERSION/$server_name"
    local download_dir="$TEMP_DIR/download/$server_name"
    local deploy_dir="$TEMP_DIR/deploy/$server_name"

    print_header "処理: $server_name"

    # Check if workspace exists in S3
    if ! aws s3 ls "s3://$S3_BUCKET/$s3_workspace_path/" --region "$AWS_REGION" &> /dev/null; then
        print_warning "Workspaceが見つかりません: s3://$S3_BUCKET/$s3_workspace_path/"
        return 1
    fi

    # Get list of world directories in S3 workspace
    local world_types=()
    while IFS= read -r line; do
        if [[ $line == *"PRE"* ]]; then
            local world_name=$(echo "$line" | awk '{print $2}' | sed 's/\///g')
            if [[ $world_name == world* ]]; then
                world_types+=("$world_name")
            fi
        fi
    done < <(aws s3 ls "s3://$S3_BUCKET/$s3_workspace_path/" --region "$AWS_REGION" 2>/dev/null)

    if [ ${#world_types[@]} -eq 0 ]; then
        print_warning "Workspaceにワールドが見つかりません: $server_name"
        return 1
    fi

    print_info "  検出されたワールド: ${world_types[*]}"

    # Create directories
    mkdir -p "$download_dir"
    mkdir -p "$deploy_dir"

    local total_size=0

    # Download, compress, and prepare for upload
    for world_type in "${world_types[@]}"; do
        local s3_world_path="s3://$S3_BUCKET/$s3_workspace_path/$world_type/"
        local local_world_path="$download_dir/$world_type"

        print_info "  📥 $world_type: ダウンロード中..."

        # Download from workspace
        aws s3 sync "$s3_world_path" "$local_world_path/" \
            --region "$AWS_REGION" \
            --quiet

        if [ $? -ne 0 ]; then
            print_error "     ダウンロード失敗: $world_type"
            return 1
        fi

        local world_size=$(du -sh "$local_world_path" 2>/dev/null | cut -f1)
        print_success "     ダウンロード完了: $world_size"

        # Compress
        print_info "  📦 $world_type: 圧縮中..."

        local archive_name="${world_type}.tar.gz"
        local archive_path="$deploy_dir/$archive_name"

        tar -cf "$archive_path" -C "$download_dir" "$world_type" --use-compress-program="gzip -$COMPRESSION_LEVEL" 2>/dev/null
        local status=$?

        if [ $status -eq 0 ] || [ $status -eq 1 ]; then
            if [ $status -eq 1 ]; then
                echo "      (ℹ️  一部のファイルが圧縮中に変更されましたが、続行します)"
            fi

            local archive_size=$(du -sh "$archive_path" | cut -f1)
            print_success "     圧縮完了: $archive_size"

            # Calculate total size
            local archive_bytes=$(du -sb "$archive_path" | cut -f1)
            total_size=$((total_size + archive_bytes))
        else
            print_error "     圧縮失敗: $world_type (ステータス: $status)"
            return 1
        fi

        # Clean up downloaded world (keep archive only)
        rm -rf "$local_world_path"
    done

    # Create metadata file
    print_info "  📝 メタデータ作成中..."

    local metadata_file="$deploy_dir/metadata.json"
    cat > "$metadata_file" << EOF
{
  "server": "$server_name",
  "year_month": "$YEAR_MONTH",
  "version": "$VERSION",
  "timestamp": "$TIMESTAMP",
  "compression_level": $COMPRESSION_LEVEL,
  "source": "workspace",
  "total_size_bytes": $total_size,
  "worlds": [
EOF

    local first=true
    for world_type in "${world_types[@]}"; do
        local archive_path="$deploy_dir/${world_type}.tar.gz"
        if [ -f "$archive_path" ]; then
            if [ "$first" = false ]; then
                echo "," >> "$metadata_file"
            fi
            first=false

            local size=$(du -sb "$archive_path" | cut -f1)
            cat >> "$metadata_file" << EOF
    {
      "world": "$world_type",
      "archive": "${world_type}.tar.gz",
      "size_bytes": $size
    }
EOF
        fi
    done

    cat >> "$metadata_file" << EOF

  ]
}
EOF

    print_success "  メタデータ作成完了"

    # Upload to S3 deployment
    if [ "$DRY_RUN" = false ]; then
        print_info "  📤 S3デプロイメントへアップロード中: s3://$S3_BUCKET/$s3_deploy_path/"

        if aws s3 sync "$deploy_dir/" "s3://$S3_BUCKET/$s3_deploy_path/" \
            --region "$AWS_REGION" \
            --no-progress; then
            print_success "  S3アップロード完了"
        else
            print_error "  S3アップロード失敗"
            return 1
        fi

        # Create __IMPORT_ENABLED__ flag
        echo "Deployed at $TIMESTAMP (from workspace)" | \
            aws s3 cp - "s3://$S3_BUCKET/$s3_deploy_path/__IMPORT_ENABLED__" \
            --region "$AWS_REGION"

        print_success "  インポートフラグ作成完了"
    else
        print_info "  (dryrun) S3アップロードをスキップ: s3://$S3_BUCKET/$s3_deploy_path/"
    fi

    echo ""
    return 0
}

# ================================================================
# Cleanup
# ================================================================

cleanup() {
    if [ -d "$TEMP_DIR" ]; then
        print_info "一時ファイルをクリーンアップ中..."
        rm -rf "$TEMP_DIR"
        print_success "クリーンアップ完了"
    fi
}

# ================================================================
# Main
# ================================================================

main() {
    clear
    print_header "Create Deployment from Workspace Script"

    # Prerequisites check
    check_prerequisites

    # バージョン自動決定
    if [ "$VERSION_MANUALLY_SET" = false ]; then
        print_info "S3から全期間の最新バージョンを確認中..."
        VERSION=$(get_next_version)
    fi

    echo "📍 Workspaceソース: s3://$S3_BUCKET/$S3_WORKSPACE_PREFIX"
    echo "📍 Deployment保存先: s3://$S3_BUCKET/$S3_DEPLOY_PREFIX$YEAR_MONTH/$VERSION/"
    echo "📅 現在の年月: $YEAR_MONTH"
    echo "🔢 デプロイバージョン: $VERSION"
    echo "🗜️  圧縮レベル: $COMPRESSION_LEVEL"
    echo "🔧 AWS リージョン: $AWS_REGION"

    if [ -n "$TARGET_SERVER" ]; then
        echo "🎯 対象サーバー: $TARGET_SERVER"
    fi

    if [ "$DRY_RUN" = true ]; then
        print_warning "🧪 ドライランモード（実際にはアップロードしません）"
    fi

    echo ""

    # Get workspace servers
    print_header "Workspaceサーバー取得"
    local servers=($(get_workspace_servers))

    if [ ${#servers[@]} -eq 0 ]; then
        if [ -n "$TARGET_SERVER" ]; then
            print_error "指定されたサーバーがWorkspaceに見つかりません: $TARGET_SERVER"
        else
            print_error "Workspaceにサーバーが見つかりません"
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
        print_warning "⚠️  Workspaceのデータをデプロイメント用に圧縮してS3にアップロードします"
        print_warning "⚠️  バージョン: $VERSION"
        echo ""
        read -p "処理を開始しますか？ (y/N): " answer
        if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
            print_info "キャンセルしました"
            exit 0
        fi
        echo ""
    fi

    # Create temp directory
    mkdir -p "$TEMP_DIR"

    # Process each server
    local success_count=0
    local fail_count=0

    for server in "${servers[@]}"; do
        if process_server "$server"; then
            success_count=$((success_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi
    done

    # Cleanup
    cleanup

    # Summary
    echo ""
    print_header "処理結果"
    echo "✅ 成功: $success_count"
    echo "❌ 失敗: $fail_count"
    echo ""

    if [ "$DRY_RUN" = false ] && [ $success_count -gt 0 ]; then
        print_success "Deploymentへの変換が完了しました！"
        echo ""
        print_info "次のステップ:"
        print_info "1. S3の内容を確認:"
        print_info "   aws s3 ls s3://$S3_BUCKET/$S3_DEPLOY_PREFIX$YEAR_MONTH/$VERSION/ --recursive --human-readable"
        print_info ""
        print_info "2. EC2でワールドデータをインポート:"
        print_info "   - servers.json で s3import: true に設定"
        print_info "   - Docker コンテナを起動"
    elif [ "$DRY_RUN" = true ]; then
        print_info "ドライランが完了しました"
        print_info "実際に処理を実行するには、--dry-run オプションを外してください"
    fi

    echo ""

    if [ $fail_count -gt 0 ]; then
        exit 1
    fi

    exit 0
}

# Trap cleanup on exit
trap cleanup EXIT

# Execute main
main
