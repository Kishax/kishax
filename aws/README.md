# AWS Infrastructure Documentation

## SSM Parameter Store ポリシー

### ssm-policy.json

全サービス共通のSSM Parameter Store アクセスポリシーです。

**対象パラメータパス:**
- `/kishax/discord/*` - Discord Bot関連設定
- `/kishax/sqs/*` - SQS関連設定
- `/kishax/gather/*` - Gather Bot関連設定
- `/kishax/slack/*` - Slack通知関連設定
- `/kishax/web/*` - Web アプリケーション関連設定

**使用方法:**
1. IAMロールまたはユーザーにこのポリシーをアタッチ
2. ECSタスク実行ロールに適用済み
3. Lambda実行ロールに適用済み

### IAMユーザー管理

#### `$(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY)`
- **用途**: Velocity MinecraftプラグインからのAPI Gateway認証
- **アクセスキーID**: `$(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY_ACCESS_KEY)`
- **アタッチポリシー**: `$(AWS_IAM_POLICY_NAME_FOR_API_GATEWAY)`
- **権限**: API Gateway (`$(AWS_API_GATEWAY_ID)`) の /discord エンドポイントへのアクセス

## Lambda Functions

### kishax-sqs-forwarder
- **用途**: API Gateway → SQS メッセージ転送
- **ランタイム**: Node.js 20.x
- **統合先**: 既存API Gateway (`$(AWS_API_GATEWAY_ID)`)
- **CloudFormationスタック**: kishax-sqs-forwarder

## API Gateway

### kishax-discord-api (`$(AWS_API_GATEWAY_ID)`)
- **用途**: Minecraft→Discord連携のメインエンドポイント
- **認証**: AWS_IAM
- **エンドポイント**:
  - POST /discord - SQS転送Lambda呼び出し

## ECS Services

### Discord Bot (discord-bot)
- **クラスター**: kishax-discord-bot-cluster
- **タスク定義**: kishax-discord-bot
- **SSM統合**: 完了

### Gather Bot (gather-bot)
- **クラスター**: kishax-gather-bot-cluster  
- **タスク定義**: kishax-gather-bot
- **SSM統合**: 完了

### Web Application (web)
- **クラスター**: kishax-web-cluster
- **タスク定義**: kishax-web
- **SSM統合**: 完了

#### API Gateway設定

メインのAPI Gateway (`$(AWS_API_GATEWAY_ID)`) の設定です。

**エンドポイント:**
- `POST /discord` - Discord Bot連携 (メイン)
- `POST /server-status` - サーバー状態通知
- `POST /player-request` - プレイヤーリクエスト
- `POST /broadcast` - ブロードキャストメッセージ

**認証:**
- 全てのPOSTエンドポイント: AWS_IAM認証
- OPTIONSメソッド: 認証なし (CORS対応)

**統合先:**
- Lambda関数: `kishax-sqs-forwarder`
- 統合タイプ: AWS_PROXY

#### SQS設定 (sqs-config.json)

Discord Bot用のSQSキューの設定です。

**メインキュー:**
- キュー名: `kishax-discord-queue`
- 可視性タイムアウト: 300秒
- メッセージ保持期間: 14日間
- Long Polling: 20秒

**Dead Letter Queue:**
- キュー名: `kishax-discord-dlq`
- 最大受信回数: 3回
- メッセージ保持期間: 14日間

## セキュリティ考慮事項

1. **SSM Parameter Store**: 全ての機密情報はSecureStringとして暗号化保存
2. **IAM最小権限**: 各サービスは必要最小限の権限のみ付与
3. **API認証**: IAM署名を使用した認証を実装
4. **ネットワーク**: VPC内でのプライベート通信を使用

---

## 統合インフラストラクチャ

### CloudFormation管理

#### kishax-infrastructure スタック
- **統合対象**: 全ECSサービス、ALB、SQS、Lambda
- **ファイル**: `aws/cloudformation-template.yaml`
- **パラメータ**: `aws/cloudformation-parameters.json`

更新日: 2025-08-22
