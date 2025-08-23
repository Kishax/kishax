# Kishax Infrastructure Deployment Guide

## 前提条件

- AWS CLI設定済み (`aws configure` または AWS SSO)
- Docker がインストール済み
- Node.js 20+ (Web app、Lambda開発用)
- Java 21+ (Minecraft plugin、Discord bot開発用)
- Make がインストール済み

## AWS 認証設定

### 1. AWS SSO設定

```bash
# SSO ログイン
aws sso login --profile $(AWS_PROFILE)
```

### 2. IAMユーザー設定 (Minecraft Plugin用)

Velocity MinecraftプラグインはIAMユーザー認証を使用します:

- **ユーザー名**: `$(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY)`
- **アクセスキーID**: `$(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY_ACCESS_KEY)`
- **ポリシー**: `$(AWS_IAM_POLICY_NAME_FOR_API_GATEWAY)`

## SSM Parameter Store 設定

全ての機密情報をSSMに設定といっても、全てを一コマンド個別で設定していくのは面倒なので、以下のように一括で設定することを推奨します。
```bash
make update-ssm
```

## 1. Infrastructure Setup

### 統合CloudFormationスタック

```bash
# 統合インフラストラクチャデプロイ
make update-infra

# または手動でCloudFormationスタック作成/更新
aws cloudformation deploy \
  --stack-name kishax-infrastructure \
  --template-file aws/cloudformation-template.yaml \
  --parameter-overrides file://aws/cloudformation-parameters.json \
  --capabilities CAPABILITY_NAMED_IAM \
  --profile $(AWS_PROFILE)

# Lambda コード更新
make deploy-lambda
```

### API Gateway権限設定

```bash
# Lambda実行権限追加
aws lambda add-permission \
  --function-name kishax-sqs-forwarder \
  --statement-id apigateway-invoke-permission \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:$(AWS_REGION):$(AWS_ACCOUNT_ID):043t******/*/*"
```

## 2. ECS Services Deployment

すべてのECSサービスは統合クラスター `kishax-infrastructure-cluster` で管理されています。

### 全サービス一括デプロイ

```bash
# 全サービスデプロイ
make deploy-all
```

### 個別サービスデプロイ

#### Discord Bot

```bash
# Makefileを使用（推奨）
make deploy-discord-bot

# または手動デプロイ
cd discord-bot
docker build -t kishax-discord-bot .
aws ecr get-login-password --region $(AWS_REGION) --profile $(AWS_PROFILE) | \
  docker login --username AWS --password-stdin $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com
docker tag kishax-discord-bot:latest $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-discord-bot:latest
docker push $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-discord-bot:latest
aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --force-new-deployment --profile $(AWS_PROFILE)
```

#### Gather Bot

```bash
# Makefileを使用（推奨）
make deploy-gather-bot

# または手動デプロイ
cd gather-bot
docker build -t kishax-gather-bot .
docker tag kishax-gather-bot:latest $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-gather-bot:latest
docker push $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-gather-bot:latest
aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --force-new-deployment --profile $(AWS_PROFILE)
```

#### Web Application

```bash
# Makefileを使用（推奨）
make deploy-web

# または手動デプロイ
cd web
npm install
npx prisma generate
docker build -t kishax-web .
docker tag kishax-web:latest $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-web:latest
docker push $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/kishax-web:latest
aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --force-new-deployment --profile $(AWS_PROFILE)
```

## 3. Minecraft Plugins

### ビルド

```bash
cd mc-plugins

# 全プラグインビルド
./gradlew build

# 個別ビルド
./gradlew :velocity:build
./gradlew :spigot:svcore:build
```

### デプロイ

```bash
# Docker コンテナを使用する場合
docker-compose up -d

# サーバー直接デプロイ
cp velocity/build/libs/velocity-*.jar /path/to/velocity/plugins/
cp spigot/svcore/build/libs/svcore-*.jar /path/to/spigot/plugins/
```

## 4. 設定ファイル

各サービスの設定ファイルテンプレートを参考に設定:

- `discord-bot/aws/task-definition.json.example`
- `gather-bot/aws/task-definition.json.example`  
- `web/aws/task-definition.json.example`
- `mc-plugins/docker/data/velocity-kishax-config.yml.example`
- `mc-plugins/docker/data/spigot-kishax-config.yml.example`

## 5. テスト・動作確認

### Lambda Function テスト

```bash
cd aws/lambda/sqs-forwarder
make test-lambda
```

### API Gateway テスト

```bash
# IAM認証付きテスト
aws apigateway test-invoke-method \
  --rest-api-id 043t****** \
  --resource-id ****** \
  --http-method POST \
  --body '{"type": "test_connection", "message": "testing"}'
```

### ECS サービス確認

```bash
# Makefileを使用（推奨）
make status-ecs
make status-lambda

# または手動確認
aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-discord-bot-service-v2 --profile $(AWS_PROFILE)
aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-gather-bot-service-v2 --profile $(AWS_PROFILE)
aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-web-service-v2 --profile $(AWS_PROFILE)
```

## トラブルシューティング

### Lambda Function

- CloudWatch Logs を確認: `/aws/lambda/kishax-sqs-forwarder`
- IAM権限を確認
- SSMパラメータアクセス権限を確認

### ECS Services

- CloudWatch Logs を確認
- タスク定義の環境変数を確認
- VPC・セキュリティグループ設定を確認

### Minecraft Plugins

- サーバーログを確認
- 設定ファイルの構文エラーを確認
- AWS認証情報を確認

## Makefileコマンド

統合インフラストラクチャに対応した新しいMakefileコマンド一覧：

### 初回セットアップ

```bash
# 前提条件チェック
make setup-prerequisites

# AWS認証設定ガイド
make setup-aws-auth

# 初回セットアップ
make setup-first-time
```

### デプロイメント

```bash
# AWS CLIを内部で使用するため、.envファイルにAWS認証情報を設定
cp .env.example .env

# 統合インフラストラクチャ管理
make update-infra
make update-ssm

# 全サービスデプロイ
make deploy-all

# 個別デプロイ
make deploy-lambda
make deploy-discord-bot
make deploy-gather-bot
make deploy-web
make build-mc-plugins

# Docker Buildx対応
make buildx-and-push service=web
```

### テスト・動作確認

```bash
make test-lambda
make test-api-gateway
make test-minecraft-discord
```

### 監視・デバッグ

```bash
# サービス状態確認
make status-ecs
make status-lambda

# ログ確認
make logs-discord-bot
make logs-gather-bot
make logs-web
make logs-lambda
```

### 開発ツール

```bash
# SSMパラメータ管理
make ssm-backup
make validate-ssm

# AWS設定ファイル管理
make setup-config-files
make validate-aws-configs

# 一時ファイル削除
make clean
```

## セキュリティ考慮事項

1. **SSM Parameter Store**: 全ての機密情報はSecureStringで暗号化
2. **IAM最小権限**: 各サービスは必要最小限の権限のみ
3. **VPC**: プライベートサブネットでサービス実行
4. **API認証**: IAM署名認証を使用

## モニタリング

- CloudWatch メトリクス・ログ
- ECS サービスメトリクス
- API Gateway メトリクス
- Lambda メトリクス

---

**最終更新**: 2025-08-21
