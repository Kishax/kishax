#!/bin/bash
set -e

# servers.jsonからVelocity設定ファイルを動的生成する

CONFIG_FILE="/mc/config/servers.json"
OUTPUT_DIR="/mc/runtime"

echo "=== Velocity Configuration Generator ==="

# velocity.tomlの[servers]セクションを生成
SERVERS_SECTION=""
TRY_LIST=""
HOME_SERVER_NAME=""

SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    INTERNAL_PORT=$(jq -r ".spigots[$i].internal_port // 25564" "$CONFIG_FILE")
    IS_HOME=$(jq -r ".spigots[$i].is_home // false" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        continue
    fi
    
    # Velocityから見たポート設定
    SERVERS_SECTION="${SERVERS_SECTION}${NAME} = \"127.0.0.1:${INTERNAL_PORT}\"\n"
    
    # is_homeがtrueの場合、tryリストの先頭に
    if [ "$IS_HOME" = "true" ]; then
        HOME_SERVER_NAME="$NAME"
        TRY_LIST="$NAME, $TRY_LIST"
    else
        TRY_LIST="$TRY_LIST$NAME, "
    fi
done

# 末尾のカンマとスペースを削除
TRY_LIST=$(echo "$TRY_LIST" | sed 's/, $//')

# velocity.toml生成用の変数を保存
# 引用符をエスケープして保存
ESCAPED_SERVERS_SECTION=$(echo -e "$SERVERS_SECTION" | sed 's/"/\\"/g')
ESCAPED_TRY_LIST=$(echo "$TRY_LIST" | sed 's/\([^,]*\)/\\"\1\\"/g' | sed 's/, /, /g')
{
    echo "VELOCITY_SERVERS_SECTION=\"${ESCAPED_SERVERS_SECTION}\""
    echo "VELOCITY_TRY_LIST=\"${ESCAPED_TRY_LIST}\""
    echo "HOME_SERVER_NAME=\"$HOME_SERVER_NAME\""
} > "$OUTPUT_DIR/velocity-config.env"

echo "Generated Velocity configuration:"
echo "  Servers: $(echo -e "$SERVERS_SECTION" | wc -l) active"
echo "  Try list: $TRY_LIST"
echo "  Home server: $HOME_SERVER_NAME"

# velocity-kishax-config.yml の Servers セクションを生成
KISHAX_SERVERS_YAML="Servers:\n"

# Proxyも追加
PROXY_COUNT=$(jq -r '.proxies | length' "$CONFIG_FILE")
for ((i=0; i<$PROXY_COUNT; i++)); do
    PROXY_NAME=$(jq -r ".proxies[$i].name" "$CONFIG_FILE")
    PROXY_TYPE=$(jq -r ".proxies[$i].type" "$CONFIG_FILE")
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}  ${PROXY_NAME}:\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    entry: false\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    platform: ${PROXY_TYPE}\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    memory: 1\n"
done

# Spigotsを追加
for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    TYPE=$(jq -r ".spigots[$i].type" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    IS_HOME=$(jq -r ".spigots[$i].is_home // false" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        continue
    fi
    
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}  ${NAME}:\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    entry: true\n"
    
    if [ "$IS_HOME" = "true" ]; then
        KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    hub: true\n"
    else
        KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    hub: false\n"
    fi
    
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    platform: spigot\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    type: null\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    modded:\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      mode: false\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      listUrl: \"\"\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      loaderType: \"\"\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      loaderUrl: \"\"\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    distributed:\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      mode: false\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}      url: \"\"\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    memory: 1\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    exec: /mc/server/home/${NAME}.sh\n"
    KISHAX_SERVERS_YAML="${KISHAX_SERVERS_YAML}    allow_prestart: true\n"
done

# YAMLを保存
echo -e "$KISHAX_SERVERS_YAML" > "$OUTPUT_DIR/kishax-servers.yml"

echo "Generated Kishax Velocity plugin Servers section"
echo "=== Configuration Generation Complete ==="




