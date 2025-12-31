#!/bin/bash
# ================================================================
# S3 World Backup Script
# ================================================================
# Backup Minecraft world data to S3 bucket on demand
#
# Usage: ./backup-world-to-s3.sh [OPTIONS]
# Location: EC2 i-a instance, inside Docker container (/mc/scripts/)
# Returns: 0=success, 1=error
# ================================================================

set -e

# ================================================================
# Configuration
# ================================================================

CONFIG_FILE="/mc/config/servers.json"
S3_BUCKET="${S3_BUCKET:-kishax-production-world-backups}"
S3_BACKUP_PREFIX="${S3_BACKUP_PREFIX:-backups/}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
BACKUP_DIR="/tmp/mc-backup-$$"
DATE=$(date +%Y%m%d)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
COMPRESSION_LEVEL=6
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
S3 World Backup Script

Usage: $0 [OPTIONS]

Options:
  --dry-run                実際にはアップロードせず、何が実行されるか確認
  --server <name>          特定サーバーのみバックアップ
  --compression <1-9>      圧縮レベル (1=速い/大きい, 9=遅い/小さい, デフォルト: 6)
  --help                   このヘルプを表示

Examples:
  $0                                    # 全サーバーをバックアップ
  $0 --dry-run                          # ドライラン
  $0 --server home                      # homeサーバーのみバックアップ
  $0 --compression 9 --server latest    # latestサーバーを最大圧縮でバックアップ

Environment Variables:
  S3_BUCKET            S3バケット名 (デフォルト: kishax-production-world-backups)
  S3_BACKUP_PREFIX     S3プレフィックス (デフォルト: backups/)
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
# Backup Single Server
# ================================================================

backup_server() {
    local server_name=$1
    local server_dir="/mc/spigot/$server_name"
    local backup_server_dir="$BACKUP_DIR/$server_name"
    local s3_backup_path="$S3_BACKUP_PREFIX$DATE/$server_name"

    print_header "バックアップ: $server_name"

    # Check if server directory exists
    if [ ! -d "$server_dir" ]; then
        print_warning "サーバーディレクトリが見つかりません: $server_dir"
        return 1
    fi

    # Create backup directory
    mkdir -p "$backup_server_dir"

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

    local backup_count=0
    local total_size=0

    for world_type in "${world_types[@]}"; do
        local world_path="$server_dir/$world_type"

        if [ ! -d "$world_path" ]; then
            print_info "  ⏭️  $world_type: ディレクトリが見つかりません（スキップ）"
            continue
        fi

        print_info "  📦 $world_type: 圧縮中..."

        # Calculate world size before compression
        local world_size=$(du -sh "$world_path" 2>/dev/null | cut -f1)
        print_info "     サイズ: $world_size"

        # Create tar.gz archive
        local archive_name="${world_type}.tar.gz"
        local archive_path="$backup_server_dir/$archive_name"

        if tar -cf "$archive_path" -C "$server_dir" "$world_type" --use-compress-program="gzip -$COMPRESSION_LEVEL" 2>/dev/null; then
            local archive_size=$(du -sh "$archive_path" | cut -f1)
            print_success "     圧縮完了: $archive_size"
            backup_count=$((backup_count + 1))

            # Calculate total backup size
            local archive_bytes=$(du -sb "$archive_path" | cut -f1)
            total_size=$((total_size + archive_bytes))
        else
            print_error "     圧縮失敗: $world_type"
            return 1
        fi
    done

    if [ $backup_count -eq 0 ]; then
        print_warning "バックアップ対象が見つかりませんでした: $server_name"
        return 1
    fi

    # Create metadata file
    print_info "  📝 メタデータ作成中..."

    local metadata_file="$backup_server_dir/metadata.json"
    cat > "$metadata_file" << EOF
{
  "server": "$server_name",
  "backup_date": "$DATE",
  "timestamp": "$TIMESTAMP",
  "compression_level": $COMPRESSION_LEVEL,
  "total_size_bytes": $total_size,
  "worlds": [
EOF

    local first=true
    for world_type in "${world_types[@]}"; do
        local archive_path="$backup_server_dir/${world_type}.tar.gz"
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

    # Upload to S3
    if [ "$DRY_RUN" = false ]; then
        print_info "  📤 S3アップロード中: s3://$S3_BUCKET/$s3_backup_path/"

        if aws s3 sync "$backup_server_dir/" "s3://$S3_BUCKET/$s3_backup_path/" \
            --region "$AWS_REGION" \
            --no-progress; then
            print_success "  S3アップロード完了"
        else
            print_error "  S3アップロード失敗"
            return 1
        fi

        # Create backup success flag
        echo "Backup completed at $TIMESTAMP" | \
            aws s3 cp - "s3://$S3_BUCKET/$s3_backup_path/__BACKUP_COMPLETED__" \
            --region "$AWS_REGION"

        print_success "  バックアップフラグ作成完了"
    else
        print_info "  (dryrun) S3アップロードをスキップ: s3://$S3_BUCKET/$s3_backup_path/"
    fi

    echo ""
    return 0
}

# ================================================================
# Cleanup
# ================================================================

cleanup() {
    if [ -d "$BACKUP_DIR" ]; then
        print_info "一時ファイルをクリーンアップ中..."
        rm -rf "$BACKUP_DIR"
        print_success "クリーンアップ完了"
    fi
}

# ================================================================
# Main
# ================================================================

main() {
    clear

    print_header "S3 World Backup Script"
    echo "📅 日付: $DATE"
    echo "📍 S3バケット: s3://$S3_BUCKET/$S3_BACKUP_PREFIX$DATE/"
    echo "🗜️  圧縮レベル: $COMPRESSION_LEVEL"
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
            print_error "バックアップ対象サーバーが見つかりません"
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
        read -p "バックアップを開始しますか？ (y/N): " answer
        if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
            print_info "キャンセルしました"
            exit 0
        fi
        echo ""
    fi

    # Create backup directory
    mkdir -p "$BACKUP_DIR"

    # Backup each server
    local success_count=0
    local fail_count=0

    for server in "${servers[@]}"; do
        if backup_server "$server"; then
            success_count=$((success_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi
    done

    # Cleanup
    cleanup

    # Summary
    echo ""
    print_header "バックアップ結果"
    echo "✅ 成功: $success_count"
    echo "❌ 失敗: $fail_count"
    echo ""

    if [ "$DRY_RUN" = false ] && [ $success_count -gt 0 ]; then
        print_success "バックアップが完了しました！"
        echo ""
        print_info "次のステップ:"
        print_info "1. S3の内容を確認:"
        print_info "   aws s3 ls s3://$S3_BUCKET/$S3_BACKUP_PREFIX$DATE/ --recursive --human-readable"
        print_info ""
        print_info "2. バックアップから復元:"
        print_info "   make backup-world-restore DATE=$DATE"
    elif [ "$DRY_RUN" = true ]; then
        print_info "ドライランが完了しました"
        print_info "実際にバックアップを実行するには、--dry-run オプションを外してください"
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
