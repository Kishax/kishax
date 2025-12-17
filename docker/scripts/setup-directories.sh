#!/bin/bash
set -e

# servers.jsonから動的にSpigot/Velocityディレクトリを生成する

CONFIG_FILE="/mc/config/servers.json"
TEMPLATE_DIR="/mc/templates"
OUTPUT_BASE="/mc"

echo "=== Dynamic Directory Generation System ==="

# Velocityディレクトリ生成
echo "Setting up Velocity directories..."
PROXY_COUNT=$(jq -r '.proxies | length' "$CONFIG_FILE")

for ((i=0; i<$PROXY_COUNT; i++)); do
    PROXY_NAME=$(jq -r ".proxies[$i].name" "$CONFIG_FILE")
    
    # Velocityは単一インスタンスを想定（複数Proxyは将来対応）
    VELOCITY_DIR="$OUTPUT_BASE/velocity"
    
    if [ ! -d "$VELOCITY_DIR" ]; then
        echo "  Creating Velocity directory: $VELOCITY_DIR"
        mkdir -p "$VELOCITY_DIR"
    else
        echo "  Velocity directory already exists: $VELOCITY_DIR"
    fi
    
    # テンプレートから設定ファイルのみをコピー（JARファイルは保持）
    if [ -d "$TEMPLATE_DIR/velocity" ]; then
        echo "  Copying Velocity config templates..."
        cp -r "$TEMPLATE_DIR/velocity/plugins" "$VELOCITY_DIR/" 2>/dev/null || true
        cp "$TEMPLATE_DIR/velocity/velocity.toml" "$VELOCITY_DIR/" 2>/dev/null || true
    fi
done

# Spigotディレクトリ生成
echo "Setting up Spigot directories..."
SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    INTERNAL_PORT=$(jq -r ".spigots[$i].internal_port" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        echo "  Skipping disabled Spigot: $NAME"
        continue
    fi
    
    # 現在は単一Spigotディレクトリ（将来的に複数対応）
    SPIGOT_DIR="$OUTPUT_BASE/spigot"
    
    if [ ! -d "$SPIGOT_DIR" ]; then
        echo "  Creating Spigot directory: $SPIGOT_DIR"
        mkdir -p "$SPIGOT_DIR"
    else
        echo "  Spigot directory already exists: $SPIGOT_DIR"
    fi
    
    # テンプレートから設定ファイルのみをコピー（JARファイルは保持）
    if [ -d "$TEMPLATE_DIR/spigot" ]; then
        echo "  Copying Spigot config templates..."
        cp -r "$TEMPLATE_DIR/spigot/config" "$SPIGOT_DIR/" 2>/dev/null || true
        cp -r "$TEMPLATE_DIR/spigot/plugins" "$SPIGOT_DIR/" 2>/dev/null || true
        cp "$TEMPLATE_DIR/spigot/server.properties" "$SPIGOT_DIR/" 2>/dev/null || true
        
        # server.propertiesのポート設定を動的に変更
        if [ -f "$SPIGOT_DIR/server.properties" ]; then
            echo "  Configuring server.properties for port $INTERNAL_PORT"
            sed -i "s/^server-port=.*/server-port=$INTERNAL_PORT/" "$SPIGOT_DIR/server.properties"
        fi
    fi
done

echo "=== Directory Generation Complete ==="


