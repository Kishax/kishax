#!/bin/bash
set -e

# servers.jsonからstatusテーブルにサーバー情報を登録する

CONFIG_FILE="/mc/config/servers.json"

# MySQLエスケープ関数
mysql_escape() {
    echo "$1" | sed "s/'/''/g"
}

echo "=== Database Server Registration ==="

# MySQL接続確認
echo "Checking MySQL connection..."
if ! mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "USE ${MYSQL_DATABASE};" 2>/dev/null; then
    echo "ERROR: Cannot connect to MySQL database"
    exit 1
fi

echo "Connected to MySQL successfully"

# statusテーブルをクリア（既存のサーバー情報を削除）
echo "Clearing existing server entries from status table..."
mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" <<EOF
DELETE FROM status WHERE 1=1;
EOF

# Proxiesを登録
echo "Registering Proxies..."
PROXY_COUNT=$(jq -r '.proxies | length' "$CONFIG_FILE")

for ((i=0; i<$PROXY_COUNT; i++)); do
    NAME=$(jq -r ".proxies[$i].name" "$CONFIG_FILE")
    TYPE=$(jq -r ".proxies[$i].type" "$CONFIG_FILE")
    
    # エスケープ処理
    NAME_ESCAPED=$(mysql_escape "$NAME")
    TYPE_ESCAPED=$(mysql_escape "$TYPE")
    
    echo "  Registering Proxy: $NAME (type: $TYPE)"
    
    mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" <<EOF
INSERT INTO status (
    name,
    port,
    online,
    platform,
    entry,
    memory,
    hub,
    allow_prestart
) VALUES (
    '$NAME_ESCAPED',
    0,
    0,
    '$TYPE_ESCAPED',
    0,
    1,
    0,
    0
);
EOF
done

# Spigotsを登録
echo "Registering Spigots..."
SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    TYPE=$(jq -r ".spigots[$i].type" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    INTERNAL_PORT=$(jq -r ".spigots[$i].internal_port // 25564" "$CONFIG_FILE")
    IS_HOME=$(jq -r ".spigots[$i].is_home // false" "$CONFIG_FILE")
    SERVER_TYPE=$(jq -r ".spigots[$i].server_type // \"survival\"" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        echo "  Skipping disabled Spigot: $NAME"
        continue
    fi
    
    # is_homeがtrueの場合はhub=1
    if [ "$IS_HOME" = "true" ]; then
        HUB=1
    else
        HUB=0
    fi
    
    # エスケープ処理
    NAME_ESCAPED=$(mysql_escape "$NAME")
    SERVER_TYPE_ESCAPED=$(mysql_escape "$SERVER_TYPE")
    
    echo "  Registering Spigot: $NAME (port: $INTERNAL_PORT, hub: $HUB, type: $SERVER_TYPE)"
    
    mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" <<EOF
INSERT INTO status (
    name,
    port,
    online,
    platform,
    entry,
    memory,
    hub,
    allow_prestart,
    \`type\`,
    \`exec\`
) VALUES (
    '$NAME_ESCAPED',
    $INTERNAL_PORT,
    0,
    'spigot',
    1,
    1,
    $HUB,
    1,
    '$SERVER_TYPE_ESCAPED',
    '/mc/server/home/${NAME_ESCAPED}.sh'
);
EOF
done

echo "=== Database Server Registration Complete ==="

# 登録されたサーバーを確認
echo ""
echo "Registered servers:"
mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" <<EOF
SELECT id, name, port, platform, entry, hub FROM status ORDER BY id;
EOF







