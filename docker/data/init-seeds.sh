#!/bin/bash

# シードファイルの初期化スクリプト
# docker/data/seeds/ ディレクトリ内のすべての*.sqlファイルを実行

echo "=== シードデータの初期化を開始 ==="

# seeds ディレクトリが存在し、SQLファイルがある場合のみ実行
if [ -d "/docker-entrypoint-initdb.d/seeds" ] && [ "$(ls -A /docker-entrypoint-initdb.d/seeds/*.sql 2>/dev/null)" ]; then
  echo "シードファイルを検出しました。実行開始..."

  # seeds ディレクトリ内の*.sqlファイルをアルファベット順で実行
  for sql_file in /docker-entrypoint-initdb.d/seeds/*.sql; do
    if [ -f "$sql_file" ]; then
      echo "実行中: $(basename "$sql_file")"
      mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" <"$sql_file"

      if [ $? -eq 0 ]; then
        echo "✓ 成功: $(basename "$sql_file")"
      else
        echo "✗ 失敗: $(basename "$sql_file")"
        exit 1
      fi
    fi
  done

  echo "=== シードデータの初期化が完了しました ==="
else
  echo "シードファイルが見つかりません。スキップします。"
fi

