# gather-slack-bot

## Env
```bash
cp .env.example .env
```

## Local Development
```bash
# 1. 依存関係インストール
npm install

# 2. 直接起動
npm start

# または
node index.js

# 3. 開発モード
npm run dev
```

## EC2
```bash
# 1. SSH接続
ssh -i ~/.ssh/gather-bot-key.pem ec2-user@xx.xxx.xxx.xxx

# 2. リポジトリクローン
git clone https://github.com/Kishax/gather-slack-bot.git
cd gather-slack-bot

# 3. 環境変数設定
cp .env.example .env
nano .env  # 実際の値を設定

# 4. Docker起動
docker-compose up -d

# 5. ログ確認
docker-compose logs -f
```


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

### Monitor
```bash
# コンテナ状況
docker stats gather-bot

# コンテナログ
docker logs -f gather-bot

# イメージ再ビルド・再起動
docker-compose up -d --build

# または手動
docker stop gather-bot
docker rm gather-bot
docker build -t gather-slack-bot .
docker run -d --name gather-bot --env-file .env gather-slack-bot

# Docker完全リセット
docker-compose down
docker system prune -f
docker-compose up -d --build
```

## License
[MIT](LICENSE)
