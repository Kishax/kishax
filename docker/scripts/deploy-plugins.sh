#!/bin/bash
set -e

# servers.jsonからプラグイン配置を動的に行う

CONFIG_FILE="/mc/config/servers.json"

# MySQLエスケープ関数
mysql_escape() {
    echo "$1" | sed "s/'/''/g"
}

echo "=== Plugin Deployment System ==="

# Spigot毎にプラグインを配置
SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    PLUGIN_PRESET=$(jq -r ".spigots[$i].plugin_preset // \"\"" "$CONFIG_FILE")
    CUSTOM_PLUGINS=$(jq -r ".spigots[$i].custom_plugins // [] | @json" "$CONFIG_FILE")
    MINECRAFT_VERSION=$(jq -r ".spigots[$i].minecraft_version // \"1.21.8\"" "$CONFIG_FILE")
    KISHAX_JAR=$(jq -r ".spigots[$i].kishax_spigot_jar // \"\"" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        echo "Spigot $i: $NAME -> DISABLED (skipping plugin deployment)"
        continue
    fi
    
    # 各サーバー固有のプラグインディレクトリ
    PLUGINS_DIR="/mc/spigot/$NAME/plugins"
    mkdir -p "$PLUGINS_DIR"
    
    echo "Deploying plugins for Spigot: $NAME (Minecraft $MINECRAFT_VERSION)"
    
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
        PLUGIN_SOURCE=$(jq -r ".plugins[\"$plugin_name\"].bukkit.source // \"download\"" "$CONFIG_FILE")
        
        if [ "$PLUGIN_SOURCE" = "build" ]; then
            echo "      Plugin is built from source: $plugin_name"
            
            # Kishaxプラグインの場合、バージョンマッピングから取得
            if [ "$plugin_name" = "kishax" ]; then
                if [ -n "$KISHAX_JAR" ]; then
                    # spigots配列で指定されたjar名を使用
                    SOURCE_JAR="/mc/build/spigot/$KISHAX_JAR"
                else
                    # version_mappingsから自動検出
                    GRADLE_MODULE=$(jq -r ".plugins.kishax.bukkit.version_mappings[\"$MINECRAFT_VERSION\"].gradle_module // null" "$CONFIG_FILE")
                    JAR_FILENAME=$(jq -r ".plugins.kishax.bukkit.version_mappings[\"$MINECRAFT_VERSION\"].jar_filename // null" "$CONFIG_FILE")
                    
                    if [ "$GRADLE_MODULE" = "null" ] || [ "$JAR_FILENAME" = "null" ]; then
                        echo "      ERROR: No version mapping for Minecraft $MINECRAFT_VERSION"
                        echo "      Available versions: $(jq -r '.plugins.kishax.bukkit.version_mappings | keys[]' "$CONFIG_FILE")"
                        continue
                    fi
                    
                    SOURCE_JAR="/mc/build/spigot/$GRADLE_MODULE/build/libs/$JAR_FILENAME"
                fi
                
                if [ -f "$SOURCE_JAR" ]; then
                    echo "      Copying Kishax plugin: $SOURCE_JAR"
                    cp "$SOURCE_JAR" "$PLUGINS_DIR/"
                else
                    echo "      ERROR: Kishax jar not found: $SOURCE_JAR"
                fi
            fi
            continue
        fi
        
        PLUGIN_URL=$(jq -r ".plugins[\"$plugin_name\"].bukkit.url // null" "$CONFIG_FILE")
        PLUGIN_FILENAME=$(jq -r ".plugins[\"$plugin_name\"].bukkit.filename // null" "$CONFIG_FILE")
        
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
        
        # ソースからビルド（Velocity用の定義を確認）
        PLUGIN_SOURCE=$(jq -r ".plugins[\"$plugin_name\"].velocity.source // .plugins[\"$plugin_name\"].source // \"download\"" "$CONFIG_FILE")
        if [ "$PLUGIN_SOURCE" = "build" ]; then
            echo "    Plugin is built from source: $plugin_name"
            
            if [ "$plugin_name" = "kishax" ]; then
                JAR_FILENAME=$(jq -r ".plugins.kishax.velocity.jar_filename // \"Kishax-Velocity-3.4.0.jar\"" "$CONFIG_FILE")
                SOURCE_JAR="/mc/build/velocity/$JAR_FILENAME"
                
                if [ -f "$SOURCE_JAR" ]; then
                    echo "    Copying Kishax Velocity plugin: $SOURCE_JAR"
                    cp "$SOURCE_JAR" "$VELOCITY_PLUGINS_DIR/"
                else
                    echo "    ERROR: Kishax Velocity jar not found: $SOURCE_JAR"
                fi
            fi
            continue
        fi
        
        # Velocityプラグインの情報を取得
        # まずは直接定義（geyser-velocity等）、次にluckpermsのようなネスト構造を試す
        PLUGIN_URL=$(jq -r ".plugins[\"$plugin_name\"].url // .plugins[\"$plugin_name\"].velocity.url // null" "$CONFIG_FILE")
        PLUGIN_FILENAME=$(jq -r ".plugins[\"$plugin_name\"].filename // .plugins[\"$plugin_name\"].velocity.filename // null" "$CONFIG_FILE")
        
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




