#!/bin/bash
# ================================================================
# S3 World Deployment Script
# ================================================================
# Deploy Minecraft world data to S3 bucket for deployment
#
# Usage: ./deploy-world-to-s3.sh [OPTIONS]
# Location: EC2 i-a instance, inside Docker container (/mc/scripts/)
# Returns: 0=success, 1=error
# ================================================================

set -e

# ================================================================
# Configuration
# ================================================================

CONFIG_FILE="/mc/config/servers.json"
S3_BUCKET="${S3_BUCKET:-kishax-production-world-backups}"
S3_DEPLOY_PREFIX="${S3_DEPLOY_PREFIX:-deployment/}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
YEAR_MONTH=$(date +%Y%m)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
VERSION="1"
VERSION_MANUALLY_SET=false
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
S3 World Deployment Script

Usage: $0 [OPTIONS]

Options:
  --dry-run                実際にはアップロードせず、何が実行されるか確認
  --server <name>          特定サーバーのみデプロイ
  --version <num>          バージョン番号 (指定しない場合は自動的に次の番号を採番)
  --help                   このヘルプを表示

Examples:
  $0                                    # 全サーバーをデプロイ (自動採番)
  $0 --dry-run                          # ドライラン
  $0 --server home                      # homeサーバーのみデプロイ
  $0 --version 5                        # 強制的にバージョン5としてデプロイ

Environment Variables:
  S3_BUCKET            S3バケット名 (デフォルト: kishax-production-world-backups)
  S3_DEPLOY_PREFIX     S3プレフィックス (デフォルト: deployment/)
  AWS_REGION           AWSリージョン (デフォルト: ap-northeast-1)
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
# Deploy Single Server (No Compression)
# ================================================================

deploy_server() {
    local server_name=$1
    local server_dir="/mc/spigot/$server_name"
    local s3_deploy_path="$S3_DEPLOY_PREFIX$YEAR_MONTH/$VERSION/$server_name"

    print_header "デプロイ: $server_name"

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

    local deploy_count=0
    local total_size=0

    for world_type in "${world_types[@]}"; do
        local world_path="$server_dir/$world_type"

        if [ ! -d "$world_path" ]; then
            print_info "  ⏭️  $world_type: ディレクトリが見つかりません（スキップ）"
            continue
        fi

        print_info "  📦 $world_type: アップロード中..."

        # Calculate world size
        local world_size=$(du -sh "$world_path" 2>/dev/null | cut -f1)
        print_info "     サイズ: $world_size"

        # Upload to S3 (no compression)
        if [ "$DRY_RUN" = false ]; then
            if aws s3 sync "$world_path/" "s3://$S3_BUCKET/$s3_deploy_path/$world_type/" \
                --region "$AWS_REGION" \
                --delete \
                --no-progress; then
                print_success "     アップロード完了"
                deploy_count=$((deploy_count + 1))

                # Calculate total size
                local world_bytes=$(du -sb "$world_path" | cut -f1)
                total_size=$((total_size + world_bytes))
            else
                print_error "     アップロード失敗: $world_type"
                return 1
            fi
        else
            print_info "     (dryrun) アップロードをスキップ"
            deploy_count=$((deploy_count + 1))
        fi
    done

    if [ $deploy_count -eq 0 ]; then
        print_warning "デプロイ対象が見つかりませんでした: $server_name"
        return 1
    fi

    # Create metadata file
    print_info "  📝 メタデータ作成中..."

    if [ "$DRY_RUN" = false ]; then
        local metadata_file="/tmp/metadata-$server_name-$$.json"
        cat > "$metadata_file" << EOF
{
  "server": "$server_name",
  "year_month": "$YEAR_MONTH",
  "version": "$VERSION",
  "timestamp": "$TIMESTAMP",
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

        aws s3 cp "$metadata_file" "s3://$S3_BUCKET/$s3_deploy_path/metadata.json" \
            --region "$AWS_REGION"
        rm -f "$metadata_file"

        print_success "  メタデータ作成完了"
    else
        print_info "  (dryrun) メタデータ作成をスキップ"
    fi

    # Create __IMPORT_ENABLED__ flag
    if [ "$DRY_RUN" = false ]; then
        print_info "  🏁 インポートフラグ作成中..."

        echo "Deployed at $TIMESTAMP" | \
            aws s3 cp - "s3://$S3_BUCKET/$s3_deploy_path/__IMPORT_ENABLED__" \
            --region "$AWS_REGION"

        print_success "  インポートフラグ作成完了"
    else
        print_info "  (dryrun) インポートフラグ作成をスキップ"
    fi

    echo ""
    return 0
}

# ================================================================
# Main
# ================================================================

main() {
    clear
    print_header "S3 World Deployment Script"

    # Prerequisites check (S3権限チェックのために先に実行)
    check_prerequisites

    # バージョン自動決定
    if [ "$VERSION_MANUALLY_SET" = false ]; then
        print_info "S3から全期間の最新バージョンを確認中..."
        VERSION=$(get_next_version)
    fi

    echo "📅 現在の年月: $YEAR_MONTH"
    echo "🔢 デプロイバージョン: $VERSION"
    echo "📍 S3保存先: s3://$S3_BUCKET/$S3_DEPLOY_PREFIX$YEAR_MONTH/$VERSION/"
    echo "🔧 AWS リージョン: $AWS_REGION"

    if [ -n "$TARGET_SERVER" ]; then
        echo "🎯 対象サーバー: $TARGET_SERVER"
    fi

    if [ "$DRY_RUN" = true ]; then
        print_warning "🧪 ドライランモード（実際にはアップロードしません）"
    fi

    echo ""

    # Get active servers
    print_header "対象サーバー取得"
    local servers=($(get_active_servers))

    if [ ${#servers[@]} -eq 0 ]; then
        if [ -n "$TARGET_SERVER" ]; then
            print_error "指定されたサーバーが見つかりません: $TARGET_SERVER"
        else
            print_error "デプロイ対象サーバーが見つかりません"
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
        print_warning "⚠️  デプロイメント用データとしてS3にアップロードします (Version: $VERSION)"
        print_warning "⚠️  このデータは import-world-from-s3.sh で使用されます"
        echo ""
        read -p "デプロイを開始しますか？ (y/N): " answer
        if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
            print_info "キャンセルしました"
            exit 0
        fi
        echo ""
    fi

    # Deploy each server
    local success_count=0
    local fail_count=0

    for server in "${servers[@]}"; do
        if deploy_server "$server"; then
            success_count=$((success_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi
    done

    # Summary
    echo ""
    print_header "デプロイ結果"
    echo "✅ 成功: $success_count"
    echo "❌ 失敗: $fail_count"
    echo ""

    if [ "$DRY_RUN" = false ] && [ $success_count -gt 0 ]; then
        print_success "デプロイが完了しました！"
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
        print_info "実際にデプロイを実行するには、--dry-run オプションを外してください"
    fi

    echo ""

    if [ $fail_count -gt 0 ]; then
        exit 1
    fi

    exit 0
}

# Execute main
main