#!/bin/bash
# ================================================================
# S3 World Data Import Script
# ================================================================
# Import Minecraft world data from S3 bucket on first server startup
#
# Usage: ./import-world-from-s3.sh <server_name>
# Returns: 0=success, 1=skipped, 2=error
# ================================================================

set -e

SERVER_NAME="$1"

if [ -z "$SERVER_NAME" ]; then
    echo "ERROR: Server name not provided"
    echo "Usage: $0 <server_name>"
    exit 2
fi

# Configuration
S3_BUCKET="${S3_BUCKET:-kishax-production-world-backups}"
S3_WORLDS_PREFIX="${S3_WORLDS_PREFIX:-deployment/}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
SERVER_DIR="/mc/spigot/${SERVER_NAME}"
VOLUME_DIR="/mc/volumes/${SERVER_NAME}"
# Store flag in world directory (persisted in volume mount)
IMPORT_COMPLETED_FLAG="$SERVER_DIR/world/.import_completed"

echo "=== S3 World Data Import ==="
echo "Server: $SERVER_NAME"
echo "S3 Bucket: s3://$S3_BUCKET/$S3_WORLDS_PREFIX"
echo "Server Directory: $SERVER_DIR"
echo "Volume Directory: $VOLUME_DIR"

# Create volume directory if not exists
mkdir -p "$VOLUME_DIR"

# Check 1: Already imported?
if [ -f "$IMPORT_COMPLETED_FLAG" ]; then
    echo "⏭️  World data already imported (flag exists: $IMPORT_COMPLETED_FLAG)"
    echo "   To force reimport: rm $IMPORT_COMPLETED_FLAG && restart container"
    exit 1
fi

# Check 2: Find latest YYYYMM directory with __IMPORT_ENABLED__ flag
echo ""
echo "Searching for latest import-enabled world data in S3..."
LATEST_S3_PATH=$(aws s3 ls "s3://$S3_BUCKET/$S3_WORLDS_PREFIX" --recursive --region "$AWS_REGION" 2>/dev/null | \
    grep "/$SERVER_NAME/__IMPORT_ENABLED__" | \
    sort -r | \
    head -1 | \
    awk '{print $4}')

if [ -z "$LATEST_S3_PATH" ]; then
    echo "⏭️  No __IMPORT_ENABLED__ flag found in S3 for server '$SERVER_NAME'"
    echo "   Skipping import. Server will start with empty/existing world."
    exit 1
fi

# Extract the directory path (remove __IMPORT_ENABLED__ filename)
S3_WORLD_DIR=$(dirname "$LATEST_S3_PATH")

# Extract year_month and version from path (deployment/YYYYMM/version/server_name)
YEAR_MONTH=$(echo "$S3_WORLD_DIR" | awk -F'/' '{print $2}')
VERSION=$(echo "$S3_WORLD_DIR" | awk -F'/' '{print $3}')

echo "✅ Found import-enabled world data:"
echo "   📅 年月: $YEAR_MONTH"
echo "   🔢 バージョン: $VERSION"
echo "   📍 S3パス: s3://$S3_BUCKET/$S3_WORLD_DIR"

# Download world data
echo ""
echo "Downloading world data from S3..."
echo "This may take several minutes depending on world size..."

# Create temp directory for download
TEMP_DOWNLOAD_DIR="/tmp/mc-world-import-$$"
mkdir -p "$TEMP_DOWNLOAD_DIR"

# Download all world directories
for WORLD_TYPE in "world" "world_nether" "world_the_end"; do
    S3_WORLD_PATH="s3://$S3_BUCKET/$S3_WORLD_DIR/$WORLD_TYPE/"
    LOCAL_WORLD_PATH="$TEMP_DOWNLOAD_DIR/$WORLD_TYPE"
    
    echo ""
    echo "Downloading $WORLD_TYPE..."
    
    # Check if world exists in S3
    if aws s3 ls "$S3_WORLD_PATH" --region "$AWS_REGION" > /dev/null 2>&1; then
        aws s3 sync "$S3_WORLD_PATH" "$LOCAL_WORLD_PATH" \
            --region "$AWS_REGION" \
            --delete \
            --quiet
        
        if [ $? -eq 0 ]; then
            echo "  ✅ Downloaded $WORLD_TYPE"
        else
            echo "  ❌ Failed to download $WORLD_TYPE"
            rm -rf "$TEMP_DOWNLOAD_DIR"
            exit 2
        fi
    else
        echo "  ⏭️  $WORLD_TYPE not found in S3, skipping"
    fi
done

# Move downloaded data to server directory
echo ""
echo "Installing world data to server directory..."
mkdir -p "$SERVER_DIR"

for WORLD_TYPE in "world" "world_nether" "world_the_end"; do
    SOURCE_PATH="$TEMP_DOWNLOAD_DIR/$WORLD_TYPE"
    DEST_PATH="$SERVER_DIR/$WORLD_TYPE"
    
    if [ -d "$SOURCE_PATH" ]; then
        # Remove existing world if present (delete contents, not directory itself for volume mounts)
        if [ -d "$DEST_PATH" ]; then
            echo "  Removing existing $WORLD_TYPE contents..."
            rm -rf "$DEST_PATH"/*
        fi
        
        # Move new world data contents (not the directory itself)
        mkdir -p "$DEST_PATH"
        mv "$SOURCE_PATH"/* "$DEST_PATH"/
        rm -rf "$SOURCE_PATH"
        echo "  ✅ Installed $WORLD_TYPE"
    fi
done

# Cleanup temp directory
rm -rf "$TEMP_DOWNLOAD_DIR"

# Create import completed flag in the server directory (persisted in volume)
echo ""
echo "Creating import completed flag..."
echo "Imported at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")" > "$IMPORT_COMPLETED_FLAG"
echo "From S3: s3://$S3_BUCKET/$S3_WORLD_DIR" >> "$IMPORT_COMPLETED_FLAG"
echo "  ✅ Flag created: $IMPORT_COMPLETED_FLAG"

echo ""
echo "=== Import Complete ==="
echo "World data successfully imported from S3"
echo "Server: $SERVER_NAME"
echo "Source: s3://$S3_BUCKET/$S3_WORLD_DIR"
echo ""

exit 0






