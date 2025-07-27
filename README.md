# gather-slack-bot

## Env
```bash
cp .env.example .env
```

## Local

### Install & Run
```bash
# 1. 依存関係インストール
npm install

# 2. PM2起動
npm run pm2:start

# または直接
pm2 start ecosystem.config.js

# 3. ステータス確認
pm2 list

# 4. ログ監視
pm2 logs gather-bot

# 5. リアルタイム監視
pm2 monit

# 6. 再起動
pm2 restart gather-bot

# 7. 停止
pm2 stop gather-bot

# 8. 削除
pm2 delete gather-bot
```

### Update
```bash
# 1. コード更新
git pull origin main

# 2. 依存関係更新
npm install

# 3. 無停止再起動
pm2 reload gather-bot

# または通常再起動
pm2 restart gather-bot


## Docker

### Build
```bash
# イメージビルド
docker build -t gather-slack-bot .

# タグ付きビルド
docker build -t gather-slack-bot:v1.0.0 .
```

### Run
```bash
# コンテナ起動
docker run -d \
  --name gather-bot \
  --env-file .env \
  --restart unless-stopped \
  gather-slack-bot

# ログ確認
docker logs gather-bot -f

# コンテナ内に入る
docker exec -it gather-bot sh

# 停止・削除
docker stop gather-bot
docker rm gather-bot
```

### Compose
```bash
# バックグラウンド起動
docker-compose up -d

# ログ監視
docker-compose logs -f

# 再起動
docker-compose restart

# 停止・削除
docker-compose down

# イメージ再ビルド
docker-compose up -d --build
```

### Monitor with pm2
```bash
# コンテナ状況
docker stats gather-bot

# リソース使用量
docker exec gather-bot pm2 monit

# コンテナ内ログ
docker exec gather-bot pm2 logs gather-bot
```

### Update
```bash
# 1. コード更新
git pull origin main

# 2. イメージ再ビルド・再起動
docker-compose up -d --build

# または手動
docker stop gather-bot
docker rm gather-bot
docker build -t gather-slack-bot .
docker run -d --name gather-bot --env-file .env gather-slack-bot
```

## pm2
```bash
# プロセス一覧
pm2 list

# 詳細情報
pm2 show gather-bot

# メモリ・CPU使用率
pm2 monit

# ログローテーション（pm2-logrotateインストール後）
pm2 install pm2-logrotate

# 1. コード更新
git pull origin main

# 2. 依存関係更新
npm install

# 3. 無停止再起動
pm2 reload gather-bot

# または通常再起動
pm2 restart gather-bot
```

## Emergency
```bash
# PM2完全リセット
pm2 kill
pm2 start ecosystem.config.js

# Docker完全リセット
docker-compose down
docker system prune -f
docker-compose up -d --build
```

## License
[MIT](LICENSE)
