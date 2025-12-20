#!/bin/bash
set -e

# servers.jsonを読み込んでメモリを計算し、起動パラメータを生成する

CONFIG_FILE="/mc/config/servers.json"
OUTPUT_DIR="/mc/runtime"

# 出力ディレクトリ作成
mkdir -p "$OUTPUT_DIR"

echo "=== Memory Calculator & Server Configurator ==="
echo "Reading configuration from: $CONFIG_FILE"

# jqがインストールされているか確認
if ! command -v jq &> /dev/null; then
    echo "ERROR: jq is not installed"
    exit 1
fi

# JSONから値を読み取る（環境変数から上書き可能）
OVERALL_MEMORY_FROM_JSON=$(jq -r '.memory.overall' "$CONFIG_FILE")
OVERALL_MEMORY=${OVERALL_MEMORY:-$OVERALL_MEMORY_FROM_JSON}
BUFFER_RATIO=$(jq -r '.memory.buffer' "$CONFIG_FILE")
MC_WANTAGE=$(jq -r '.memory.mc_wantage' "$CONFIG_FILE")

# Buffer Memoryを計算（パーセンテージとして扱う）
BUFFER_MEMORY=$(echo "scale=2; $OVERALL_MEMORY * $BUFFER_RATIO" | bc)

echo "Overall Memory: ${OVERALL_MEMORY} GB"
echo "Buffer Ratio: ${BUFFER_RATIO} (${BUFFER_MEMORY} GB)"
echo "MC Wantage: ${MC_WANTAGE}"

# MC全体メモリ計算: O-MC = (O-M - B-M) × O-MC-WANTAGE
MC_TOTAL_MEMORY=$(echo "scale=2; ($OVERALL_MEMORY - $BUFFER_MEMORY) * $MC_WANTAGE" | bc)
echo "MC Total Memory (O-MC): ${MC_TOTAL_MEMORY} GB"

# Proxiesのメモリ比率合計を計算
PROXIES_RATIO_SUM=$(jq -r '[.proxies[].memory_ratio] | add' "$CONFIG_FILE")
echo "Proxies Memory Ratio Sum: ${PROXIES_RATIO_SUM}"

# Spigotsのメモリ比率合計を計算（memory_ratio > 0のものだけ）
SPIGOTS_RATIO_SUM=$(jq -r '[.spigots[] | select(.memory_ratio > 0) | .memory_ratio] | add' "$CONFIG_FILE")
echo "Spigots Memory Ratio Sum (active only): ${SPIGOTS_RATIO_SUM}"

# 合計比率
TOTAL_RATIO=$(echo "scale=4; $PROXIES_RATIO_SUM + $SPIGOTS_RATIO_SUM" | bc)
echo "Total Memory Ratio: ${TOTAL_RATIO}"

# 比率合計チェック
if (( $(echo "$TOTAL_RATIO > 1.0" | bc -l) )); then
    echo "ERROR: Total memory ratio (${TOTAL_RATIO}) exceeds 1.0"
    exit 1
fi

# 残りメモリがある場合は均等配分
REMAINING_RATIO=$(echo "scale=4; 1.0 - $TOTAL_RATIO" | bc)
ACTIVE_SERVER_COUNT=$(jq -r '([.proxies | length] + [.spigots[] | select(.memory_ratio > 0)] | length)' "$CONFIG_FILE")

if (( $(echo "$REMAINING_RATIO > 0" | bc -l) )); then
    BONUS_RATIO_PER_SERVER=$(echo "scale=6; $REMAINING_RATIO / $ACTIVE_SERVER_COUNT" | bc)
    echo "Remaining Memory Ratio: ${REMAINING_RATIO}"
    echo "Bonus per active server: ${BONUS_RATIO_PER_SERVER}"
else
    BONUS_RATIO_PER_SERVER=0
    echo "No remaining memory to distribute"
fi

# Proxies設定ファイル生成
echo "" > "$OUTPUT_DIR/proxies.env"
PROXY_COUNT=$(jq -r '.proxies | length' "$CONFIG_FILE")
for ((i=0; i<$PROXY_COUNT; i++)); do
    NAME=$(jq -r ".proxies[$i].name" "$CONFIG_FILE")
    TYPE=$(jq -r ".proxies[$i].type" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".proxies[$i].memory_ratio" "$CONFIG_FILE")
    FILENAME=$(jq -r ".proxies[$i].filename" "$CONFIG_FILE")
    
    # 実メモリ計算（ボーナス含む）
    ALLOCATED_RATIO=$(echo "scale=6; $MEMORY_RATIO + $BONUS_RATIO_PER_SERVER" | bc)
    ACTUAL_MEMORY=$(echo "scale=2; $MC_TOTAL_MEMORY * $ALLOCATED_RATIO" | bc)
    ACTUAL_MEMORY_INT=$(printf "%.0f" "$ACTUAL_MEMORY")
    
    echo "PROXY_${i}_NAME=\"$NAME\"" >> "$OUTPUT_DIR/proxies.env"
    echo "PROXY_${i}_TYPE=\"$TYPE\"" >> "$OUTPUT_DIR/proxies.env"
    echo "PROXY_${i}_MEMORY=\"${ACTUAL_MEMORY_INT}G\"" >> "$OUTPUT_DIR/proxies.env"
    echo "PROXY_${i}_FILENAME=\"$FILENAME\"" >> "$OUTPUT_DIR/proxies.env"
    
    echo "Proxy $i: $NAME -> ${ACTUAL_MEMORY} GB (${ACTUAL_MEMORY_INT}G)"
done

# Spigots設定ファイル生成
echo "" > "$OUTPUT_DIR/spigots.env"
SPIGOT_COUNT=$(jq -r '.spigots | length' "$CONFIG_FILE")
ACTIVE_SPIGOT_INDEX=0

for ((i=0; i<$SPIGOT_COUNT; i++)); do
    NAME=$(jq -r ".spigots[$i].name" "$CONFIG_FILE")
    TYPE=$(jq -r ".spigots[$i].type" "$CONFIG_FILE")
    MEMORY_RATIO=$(jq -r ".spigots[$i].memory_ratio" "$CONFIG_FILE")
    FILENAME=$(jq -r ".spigots[$i].filename" "$CONFIG_FILE")
    PORT=$(jq -r ".spigots[$i].port" "$CONFIG_FILE")
    IS_HOME=$(jq -r ".spigots[$i].is_home // false" "$CONFIG_FILE")
    
    # memory_ratio が 0 の場合はスキップ
    if (( $(echo "$MEMORY_RATIO == 0" | bc -l) )); then
        echo "Spigot $i: $NAME -> DISABLED (memory_ratio = 0)"
        continue
    fi
    
    # 実メモリ計算（ボーナス含む）
    ALLOCATED_RATIO=$(echo "scale=6; $MEMORY_RATIO + $BONUS_RATIO_PER_SERVER" | bc)
    ACTUAL_MEMORY=$(echo "scale=2; $MC_TOTAL_MEMORY * $ALLOCATED_RATIO" | bc)
    ACTUAL_MEMORY_INT=$(printf "%.0f" "$ACTUAL_MEMORY")
    
    echo "SPIGOT_${ACTIVE_SPIGOT_INDEX}_NAME=\"$NAME\"" >> "$OUTPUT_DIR/spigots.env"
    echo "SPIGOT_${ACTIVE_SPIGOT_INDEX}_TYPE=\"$TYPE\"" >> "$OUTPUT_DIR/spigots.env"
    echo "SPIGOT_${ACTIVE_SPIGOT_INDEX}_MEMORY=\"${ACTUAL_MEMORY_INT}G\"" >> "$OUTPUT_DIR/spigots.env"
    echo "SPIGOT_${ACTIVE_SPIGOT_INDEX}_FILENAME=\"$FILENAME\"" >> "$OUTPUT_DIR/spigots.env"
    echo "SPIGOT_${ACTIVE_SPIGOT_INDEX}_PORT=\"$PORT\"" >> "$OUTPUT_DIR/spigots.env"
    
    # is_home が true の場合、HOME_SERVER環境変数を設定
    if [ "$IS_HOME" = "true" ]; then
        echo "HOME_SERVER_NAME=\"$NAME\"" >> "$OUTPUT_DIR/spigots.env"
        echo "HOME_SERVER_IP=\"127.0.0.1\"" >> "$OUTPUT_DIR/spigots.env"
        echo "Spigot $i: $NAME -> ${ACTUAL_MEMORY} GB (${ACTUAL_MEMORY_INT}G) on port $PORT [HOME SERVER]"
    else
        echo "Spigot $i: $NAME -> ${ACTUAL_MEMORY} GB (${ACTUAL_MEMORY_INT}G) on port $PORT"
    fi
    
    ACTIVE_SPIGOT_INDEX=$((ACTIVE_SPIGOT_INDEX + 1))
done

# アクティブなサーバー数を記録
echo "ACTIVE_PROXY_COUNT=$PROXY_COUNT" >> "$OUTPUT_DIR/proxies.env"
echo "ACTIVE_SPIGOT_COUNT=$ACTIVE_SPIGOT_INDEX" >> "$OUTPUT_DIR/spigots.env"

echo "=== Configuration Complete ==="
echo "Generated files:"
echo "  - $OUTPUT_DIR/proxies.env"
echo "  - $OUTPUT_DIR/spigots.env"




