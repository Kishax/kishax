#!/bin/bash
set -e

# servers.jsonからプラグイン配置を動的に行う

CONFIG_FILE="/mc/config/servers.json"

echo "=== Plugin Deployment System ==="

# Spigot毎にプラグインを配置
SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    PLUGIN_PRESET=$(jq -r ".spigots[$i].plugin_preset // \"\"" "$CONFIG_FILE")
    CUSTOM_PLUGINS=$(jq -r ".spigots[$i].custom_plugins // [] | @json" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        echo "Spigot $i: $NAME -> DISABLED (skipping plugin deployment)"
        continue
    fi
    
    # プラグインディレクトリ作成（将来的に複数Spigot対応）
    # 現在は単一spigotディレクトリだが、将来的には/mc/spigots/${NAME}/plugins/にする
    PLUGINS_DIR="/mc/spigot/plugins"
    mkdir -p "$PLUGINS_DIR"
    
    echo "Deploying plugins for Spigot: $NAME"
    
    # custom_pluginsが指定されている場合はそれを使用、なければpreset使用
    if [ "$CUSTOM_PLUGINS" != "[]" ] && [ "$CUSTOM_PLUGINS" != "null" ]; then
        echo "  Using custom plugins list"
        PLUGIN_LIST=$(echo "$CUSTOM_PLUGINS" | jq -r '.[]')
    elif [ -n "$PLUGIN_PRESET" ] && [ "$PLUGIN_PRESET" != "null" ]; then
        echo "  Using plugin preset: $PLUGIN_PRESET"
        PLUGIN_LIST=$(jq -r ".plugin_presets[\"$PLUGIN_PRESET\"].plugins[]" "$CONFIG_FILE")
    else
        echo "  No plugins specified, skipping"
        continue
    fi
    
    # プラグインをダウンロード/配置
    for plugin_name in $PLUGIN_LIST; do
        echo "    Processing plugin: $plugin_name"
        
        # プラグインがservers.jsonに定義されているか確認
        PLUGIN_EXISTS=$(jq -r ".plugins[\"$plugin_name\"] // null" "$CONFIG_FILE")
        
        if [ "$PLUGIN_EXISTS" = "null" ]; then
            echo "      WARNING: Plugin '$plugin_name' not found in servers.json, skipping"
            continue
        fi
        
        # Bukkitプラグインの情報を取得
        PLUGIN_URL=$(jq -r ".plugins[\"$plugin_name\"].bukkit.url // null" "$CONFIG_FILE")
        PLUGIN_FILENAME=$(jq -r ".plugins[\"$plugin_name\"].bukkit.filename // null" "$CONFIG_FILE")
        PLUGIN_SOURCE=$(jq -r ".plugins[\"$plugin_name\"].bukkit.source // \"download\"" "$CONFIG_FILE")
        
        if [ "$PLUGIN_SOURCE" = "build" ]; then
            echo "      Plugin is built from source (already handled by Dockerfile)"
            continue
        fi
        
        if [ "$PLUGIN_URL" = "null" ] || [ "$PLUGIN_FILENAME" = "null" ]; then
            echo "      WARNING: Plugin '$plugin_name' missing URL or filename, skipping"
            continue
        fi
        
        # プラグインが既に存在する場合はスキップ
        if [ -f "$PLUGINS_DIR/$PLUGIN_FILENAME" ]; then
            echo "      Plugin already exists: $PLUGIN_FILENAME"
            continue
        fi
        
        # プラグインをダウンロード
        echo "      Downloading: $PLUGIN_FILENAME from $PLUGIN_URL"
        wget -q -O "$PLUGINS_DIR/$PLUGIN_FILENAME" "$PLUGIN_URL"
        
        if [ $? -eq 0 ]; then
            echo "      ✓ Successfully downloaded: $PLUGIN_FILENAME"
        else
            echo "      ✗ Failed to download: $PLUGIN_FILENAME"
        fi
    done
    
    echo "  Plugin deployment completed for $NAME"
done

# Velocity プラグイン配置（全Proxy共通）
echo ""
echo "Deploying Velocity plugins..."

VELOCITY_PLUGINS_DIR="/mc/velocity/plugins"
mkdir -p "$VELOCITY_PLUGINS_DIR"

PROXY_COUNT=$(jq -r '.proxies | length' "$CONFIG_FILE")

if [ $PROXY_COUNT -gt 0 ]; then
    # 最初のProxyのプラグインリストを使用（全Proxy共通と仮定）
    VELOCITY_PLUGIN_LIST=$(jq -r '.proxies[0].plugins[]' "$CONFIG_FILE")
    
    for plugin_name in $VELOCITY_PLUGIN_LIST; do
        echo "  Processing Velocity plugin: $plugin_name"
        
        # プラグインがservers.jsonに定義されているか確認
        PLUGIN_EXISTS=$(jq -r ".plugins[\"$plugin_name\"] // null" "$CONFIG_FILE")
        
        if [ "$PLUGIN_EXISTS" = "null" ]; then
            echo "    WARNING: Plugin '$plugin_name' not found in servers.json, skipping"
            continue
        fi
        
        # Velocityプラグインの情報を取得（直接定義の場合）
        PLUGIN_URL=$(jq -r ".plugins[\"$plugin_name\"].url // null" "$CONFIG_FILE")
        PLUGIN_FILENAME=$(jq -r ".plugins[\"$plugin_name\"].filename // null" "$CONFIG_FILE")
        
        if [ "$PLUGIN_URL" = "null" ] || [ "$PLUGIN_FILENAME" = "null" ]; then
            echo "    WARNING: Plugin '$plugin_name' missing URL or filename, skipping"
            continue
        fi
        
        # プラグインが既に存在する場合はスキップ
        if [ -f "$VELOCITY_PLUGINS_DIR/$PLUGIN_FILENAME" ]; then
            echo "    Plugin already exists: $PLUGIN_FILENAME"
            continue
        fi
        
        # プラグインをダウンロード
        echo "    Downloading: $PLUGIN_FILENAME from $PLUGIN_URL"
        wget -q -O "$VELOCITY_PLUGINS_DIR/$PLUGIN_FILENAME" "$PLUGIN_URL"
        
        if [ $? -eq 0 ]; then
            echo "    ✓ Successfully downloaded: $PLUGIN_FILENAME"
        else
            echo "    ✗ Failed to download: $PLUGIN_FILENAME"
        fi
    done
fi

echo "=== Plugin Deployment Complete ==="
