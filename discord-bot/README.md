# Kishax Discord Bot (AWS版)

Kishax MinecraftサーバーのDiscord Bot AWS実装版。  
リフレクション依存のJDA実装から、スケーラブルなAWSアーキテクチャに移行。

> ⚠️ **重要**: 本リポジトリには機密情報を含むファイルが存在します。  
> 必ず `.env.example` からコピーして設定し、実際の認証情報は `.gitignore` で除外されています。

## アーキテクチャ

```
Velocity Java Plugin → API Gateway → Lambda → SQS → ECS(Discord Bot)
```

### 構成要素

- **API Gateway**: 外部からのリクエストを受付
- **Lambda**: リクエストを処理してSQSにメッセージを送信
- **SQS**: 非同期処理キュー（メッセージの平準化）
- **ECS Fargate**: Discord Bot常駐（WebSocket接続維持）

## 特徴

- ✅ **リフレクション不使用**: JDAライブラリを直接使用
- ✅ **AWS Native**: ECS + SQS + API Gateway
- ✅ **スケーラブル**: 各サービスが独立してスケール可能
- ✅ **安定接続**: ECSで安定したWebSocket接続維持
- ✅ **コスト最適**: Lambdaは瞬間処理で課金最小

## プロジェクト構造

```
discord-bot/
├── src/main/java/net/kishax/discord/
│   ├── DiscordBotMain.java          # メインクラス
│   ├── Config.java                  # 設定管理
│   ├── DiscordEventListener.java    # イベント処理
│   ├── CommandRegistrar.java        # コマンド登録
│   └── SqsMessageProcessor.java     # SQS処理
├── src/main/resources/
│   ├── application.conf             # 設定ファイル
│   └── logback.xml                  # ログ設定
├── aws/
│   ├── cloudformation-template.yaml # インフラ定義
│   ├── task-definition.json         # ECSタスク定義
│   ├── service-definition.json      # ECSサービス定義
│   ├── lambda/                      # Lambda関数
│   └── sqs-config.json             # SQS設定
├── Dockerfile                       # Dockerイメージ定義
└── build.gradle                     # ビルド設定
```

## 🚀 セットアップ

### 1. 設定ファイル作成

```bash
# 環境変数ファイル作成
cp .env.example .env

# AWS設定ファイル作成  
cp aws/cloudformation-parameters.json.example aws/cloudformation-parameters.json
cp aws/task-definition.json.example aws/task-definition.json

# Velocity側設定
cp ../velocity/src/main/resources/config.yml.example ../velocity/src/main/resources/config.yml
```

### 2. Discord Bot設定

1. [Discord Developer Portal](https://discord.com/developers/applications) でBot作成
2. `.env` ファイルにToken等を設定:

```bash
# Discord設定
DISCORD_TOKEN=YOUR_DISCORD_BOT_TOKEN_HERE
DISCORD_CHANNEL_ID=YOUR_DISCORD_CHANNEL_ID
DISCORD_CHAT_CHANNEL_ID=YOUR_DISCORD_CHAT_CHANNEL_ID
DISCORD_ADMIN_CHANNEL_ID=YOUR_DISCORD_ADMIN_CHANNEL_ID  
DISCORD_RULE_CHANNEL_ID=YOUR_DISCORD_RULE_CHANNEL_ID
DISCORD_RULE_MESSAGE_ID=YOUR_DISCORD_RULE_MESSAGE_ID
DISCORD_GUILD_ID=YOUR_DISCORD_GUILD_ID
DISCORD_EMOJI_DEFAULT_NAME=steve
DISCORD_PRESENCE_ACTIVITY=Kishaxサーバー

# AWS設定
AWS_REGION=ap-northeast-1
SQS_QUEUE_URL=https://sqs.ap-northeast-1.amazonaws.com/ACCOUNT_ID/kishax-discord-queue
```

### 2. ローカル実行

```bash
# ビルド
./gradlew build

# 実行
java -jar build/libs/discord-bot-1.0.0.jar
```

### 3. Docker実行

```bash
# イメージビルド
docker build -t kishax-discord-bot .

# コンテナ実行
docker run -e DISCORD_TOKEN="..." -e SQS_QUEUE_URL="..." kishax-discord-bot
```

## AWS デプロイ

### 1. CloudFormation でインフラ構築

```bash
aws cloudformation create-stack \
  --stack-name kishax-discord-bot \
  --template-body file://aws/cloudformation-template.yaml \
  --parameters file://aws/cloudformation-parameters.json \
  --capabilities CAPABILITY_IAM
```

### 2. ECR にイメージをプッシュ

```bash
# ECRログイン
aws ecr get-login-password --region ap-northeast-1 | docker login --username AWS --password-stdin ACCOUNT_ID.dkr.ecr.ap-northeast-1.amazonaws.com

# リポジトリ作成
aws ecr create-repository --repository-name kishax-discord-bot --region ap-northeast-1

# イメージタグ付け
docker tag kishax-discord-bot:latest ACCOUNT_ID.dkr.ecr.ap-northeast-1.amazonaws.com/kishax-discord-bot:latest

# プッシュ
docker push ACCOUNT_ID.dkr.ecr.ap-northeast-1.amazonaws.com/kishax-discord-bot:latest
```

### 3. ECS サービス作成

```bash
# タスク定義登録
aws ecs register-task-definition --cli-input-json file://aws/task-definition.json

# サービス作成
aws ecs create-service --cli-input-json file://aws/service-definition.json
```

## メッセージタイプ

### サーバーステータス更新

```json
{
  "type": "server_status",
  "serverName": "survival",
  "status": "online"
}
```

### プレイヤーリクエスト

```json
{
  "type": "player_request",
  "playerName": "PlayerName",
  "playerUUID": "uuid-here",
  "serverName": "survival",
  "requestId": "req-123"
}
```

### ブロードキャストメッセージ

```json
{
  "type": "broadcast",
  "content": "サーバーメンテナンスのお知らせ",
  "isChat": false
}
```

## Discord コマンド

- `/kishax image_add_q` - 画像マップをキューに追加
- `/kishax syncrulebook` - ルールブック同期

## ログ

ログは以下の場所に出力されます：
- コンソール: 標準出力
- ファイル: `logs/discord-bot.log`
- CloudWatch: `/ecs/kishax-discord-bot` ロググループ

## 🔒 セキュリティ注意事項

### 機密情報管理

- `.env` ファイルは **絶対にコミットしない**
- AWS認証情報は **本番環境ではIAMロール** を使用
- Discord Tokenは **SSM Parameter Store** で管理
- `.gitignore` で除外されているファイルを確認

### 推奨設定

```bash
# 本番環境では環境変数でAWS認証
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
# → IAMロールで自動認証

# SSM Parameter Storeを使用
aws ssm put-parameter --name "/kishax/discord/token" --value "token" --type "SecureString"
```

## 🔍 トラブルシューティング

### ECS関連

```bash
# ECS タスク起動失敗時のログ確認
aws logs get-log-events \
  --log-group-name "/ecs/kishax-discord-bot" \
  --log-stream-name "ecs/discord-bot/TASK_ID"

# タスク状態確認
aws ecs describe-tasks \
  --cluster kishax-discord-bot-cluster \
  --tasks TASK_ARN
```

### SQS関連

```bash
# SQS キュー状態確認
aws sqs get-queue-attributes \
  --queue-url "YOUR_SQS_QUEUE_URL" \
  --attribute-names All

# DLQ メッセージ確認
aws sqs receive-message \
  --queue-url "YOUR_DLQ_URL"
```

### API Gateway関連

```bash
# Lambda ログ確認
aws logs get-log-events \
  --log-group-name "/aws/lambda/kishax-discord-api"

# API Gateway アクセスログ確認 
aws logs get-log-events \
  --log-group-name "API-Gateway-Execution-Logs_YOUR_API_ID/prod"
```

### よくある問題

1. **Discord Tokenエラー**
   - `.env` ファイルまたはSSMパラメータの確認
   - Bot権限の確認

2. **SQS接続エラー**
   - IAMロール・ポリシーの確認
   - VPC設定（プライベートサブネット使用時）

3. **ECS起動失敗**
   - タスク定義のリソース設定
   - セキュリティグループのアウトバウンド許可
   - ECRイメージの存在確認

## 開発

### ローカル開発環境

```bash
# 依存関係インストール
./gradlew build

# テスト実行
./gradlew test

# アプリケーション実行
./gradlew run
```