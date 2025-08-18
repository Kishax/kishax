# GitHub Actions 自動デプロイ設定ガイド

このドキュメントでは、KishaXプロジェクトでGitHub Actions を使用したECS Fargateへの自動デプロイを設定する方法を説明します。

## 📋 概要

GitHub Actions を使用することで、`master` ブランチへのプッシュ時に自動的に：

1. Docker イメージをビルド
2. Amazon ECR にプッシュ
3. ECS Fargate サービスを更新
4. Slack 通知（オプション）

## 🚀 クイックスタート

### 1. 前提条件確認

以下が完了していることを確認してください：

- [x] ECS Fargate デプロイメント完了（`DEPLOY-WITH-ECS-FARGATE.md` 参照）
- [x] AWS IAM ユーザー作成済み
- [x] ECR リポジトリ作成済み
- [x] GitHub リポジトリの Admin 権限

### 2. GitHub Secrets 設定

リポジトリの `Settings` > `Secrets and variables` > `Actions` で以下を設定：

#### 必須 Secrets

> ⚠️ **注意**: 以下の値は例です。実際の値は、インフラ担当者から別途提供されます。

| Secret 名 | 値（例） | 説明 |
|-----------|-----|------|
| `AWS_ACCESS_KEY_ID` | `AKIA****************` | GitHub Actions 専用 IAM ユーザーのアクセスキー |
| `AWS_SECRET_ACCESS_KEY` | `************************************` | GitHub Actions 専用 IAM ユーザーのシークレットキー |
| `AWS_ACCOUNT_ID` | `123456789012` | AWS アカウント ID |
| `SECRETS_SUFFIX` | `XXXXXX` | Secrets Manager のサフィックス |

#### オプション Secrets

| Secret 名 | 値 | 説明 |
|-----------|-----|------|
| `SLACK_WEBHOOK_URL` | `https://hooks.slack.com/services/...` | デプロイ通知用 Slack Webhook URL |

## 🔧 詳細設定手順

### ステップ 1: GitHub Secrets の設定

1. **GitHub リポジトリにアクセス**
   ```
   https://github.com/your-username/kishax-nextjs
   ```

2. **Settings タブをクリック**

3. **左サイドバーから「Secrets and variables」→「Actions」を選択**

4. **「New repository secret」をクリック**

5. **各 Secret を順番に追加**

   > 💡 **ヒント**: 実際の値はインフラ担当者から提供されたものを使用してください。

   **AWS_ACCESS_KEY_ID の設定:**
   ```
   Name: AWS_ACCESS_KEY_ID
   Secret: AKIA****************
   ```

   **AWS_SECRET_ACCESS_KEY の設定:**
   ```
   Name: AWS_SECRET_ACCESS_KEY
   Secret: ************************************
   ```

   **AWS_ACCOUNT_ID の設定:**
   ```
   Name: AWS_ACCOUNT_ID
   Secret: 123456789012
   ```

   **SECRETS_SUFFIX の設定:**
   ```
   Name: SECRETS_SUFFIX
   Secret: XXXXXX
   ```

### ステップ 2: Slack 通知設定（オプション）

デプロイの成功/失敗を Slack に通知したい場合：

1. **Slack Webhook URL を取得**
   - Slack アプリで Incoming Webhooks を有効化
   - Webhook URL をコピー

2. **GitHub Secret に追加**
   ```
   Name: SLACK_WEBHOOK_URL
   Secret: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
   ```

### ステップ 3: GitHub Actions の動作確認

1. **workflow ファイルの確認**
   ```
   .github/workflows/deploy.yml
   ```

2. **テストデプロイ実行**
   - `master` ブランチに変更をプッシュ
   - GitHub の「Actions」タブで実行状況を確認

## 🔒 セキュリティ考慮事項

### IAM ユーザーの最小権限設定

作成済みの `github-actions-deploy` ユーザーは以下の最小権限のみを保持：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeTasks",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["iam:PassRole"],
      "Resource": [
        "arn:aws:iam::126112056177:role/ecsTaskExecutionRole",
        "arn:aws:iam::126112056177:role/AppRunnerInstanceRole"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:ap-northeast-1:126112056177:log-group:/ecs/kishax-web*"
    }
  ]
}
```

### 機密情報の保護

- ✅ **task-definition.json** は `.gitignore` で除外済み
- ✅ **実際の AWS アカウント ID** は GitHub Secrets で管理
- ✅ **IAM ポリシー** は最小権限の原則に従う
- ✅ **テンプレートファイル** のみを Git で管理

## 🔄 GitHub Actions ワークフロー詳細

### トリガー条件

```yaml
on:
  push:
    branches: [master]  # master ブランチへのプッシュ
  workflow_dispatch:    # 手動実行
```

### 実行ステップ

1. **コードチェックアウト**
2. **AWS 認証情報設定**
3. **ECR ログイン**
4. **Docker イメージビルド・プッシュ**
5. **タスク定義生成**（テンプレートから動的生成）
6. **ECS サービス更新**
7. **Slack 通知**（成功/失敗）

### 環境変数

```yaml
env:
  AWS_REGION: ap-northeast-1
  AWS_ACCOUNT_ID: ${{ secrets.AWS_ACCOUNT_ID }}
  ECR_REPOSITORY: kishax-web
  ECS_SERVICE: kishax-web-service
  ECS_CLUSTER: kishax-cluster
  ECS_TASK_DEFINITION: kishax-web-task
  CONTAINER_NAME: kishax-web
```

## 📊 モニタリング・ログ確認

### GitHub Actions ログ

1. **GitHub リポジトリの「Actions」タブ**
2. **実行中/完了したワークフローをクリック**
3. **各ステップの詳細ログを確認**

### AWS ECS ログ

```bash
# ECS サービス状態確認
aws ecs describe-services \
  --cluster kishax-cluster \
  --services kishax-web-service \
  --profile AdministratorAccess-126112056177

# CloudWatch ログ確認
aws logs filter-log-events \
  --log-group-name "/ecs/kishax-web" \
  --start-time $(date -d '10 minutes ago' +%s)000 \
  --profile AdministratorAccess-126112056177
```

### Slack 通知例

**成功時:**
```
✅ デプロイが完了しました！
Branch: master
Commit: a1b2c3d
Duration: 3m 42s
```

**失敗時:**
```
❌ デプロイに失敗しました
Branch: master  
Commit: a1b2c3d
Error: Task definition registration failed
```

## 🚨 トラブルシューティング

### よくある問題と解決法

#### 1. AWS 認証エラー

**症状:**
```
Error: The security token included in the request is invalid
```

**解決法:**
```bash
# GitHub Secrets の確認
- AWS_ACCESS_KEY_ID が正しいか確認
- AWS_SECRET_ACCESS_KEY が正しいか確認
- IAM ユーザーが有効か確認
```

#### 2. ECR プッシュ失敗

**症状:**
```
Error: denied: requested access to the resource is denied
```

**解決法:**
```bash
# ECR 権限確認
aws ecr describe-repositories --repository-names kishax-web \
  --profile AdministratorAccess-126112056177

# IAM ポリシー確認
aws iam list-attached-user-policies --user-name github-actions-deploy \
  --profile AdministratorAccess-126112056177
```

#### 3. ECS デプロイ失敗

**症状:**
```
Error: Service update failed
```

**解決法:**
```bash
# ECS サービス状態確認
aws ecs describe-services \
  --cluster kishax-cluster \
  --services kishax-web-service \
  --profile AdministratorAccess-126112056177

# タスク定義確認
aws ecs describe-task-definition \
  --task-definition kishax-web-task \
  --profile AdministratorAccess-126112056177
```

#### 4. Slack 通知失敗

**症状:**
```
Error: Slack notification failed
```

**解決法:**
```bash
# Webhook URL の確認
- SLACK_WEBHOOK_URL が正しいか確認
- Slack アプリの権限が有効か確認
- チャンネルが存在するか確認
```

### デバッグ用コマンド

```bash
# GitHub Actions ローカル実行（act使用）
act push

# Docker イメージ手動ビルド
docker build -t kishax-web:test .

# ECS タスク手動実行
aws ecs run-task \
  --cluster kishax-cluster \
  --task-definition kishax-web-task \
  --launch-type FARGATE \
  --network-configuration 'awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}' \
  --profile AdministratorAccess-126112056177
```

## 🔄 ワークフロー カスタマイズ

### デプロイ頻度の調整

```yaml
# 特定の時間のみデプロイ実行
on:
  schedule:
    - cron: '0 9 * * 1-5'  # 平日 9:00 AM (UTC)
```

### 環境別デプロイ

```yaml
# ブランチ別環境分け
jobs:
  deploy-staging:
    if: github.ref == 'refs/heads/develop'
    # staging 環境用設定

  deploy-production:
    if: github.ref == 'refs/heads/master'
    # production 環境用設定
```

### 手動承認ステップ

```yaml
jobs:
  approval:
    runs-on: ubuntu-latest
    environment: production  # 手動承認が必要な環境
```

## 📚 参考リンク

- [GitHub Actions ドキュメント](https://docs.github.com/en/actions)
- [AWS ECS GitHub Actions](https://github.com/aws-actions/amazon-ecs-deploy-task-definition)
- [Docker build-push-action](https://github.com/docker/build-push-action)
- [Slack GitHub Action](https://github.com/8398a7/action-slack)

## 🔄 更新履歴

| 日付 | 変更内容 |
|------|----------|
| 2025-08-18 | 初版作成、ECS Fargate 対応 |
| 2025-08-18 | セキュリティ強化、最小権限 IAM 設定 |
| 2025-08-18 | テンプレート化、機密情報保護 |

---

💡 **ヒント:** 初回設定後は、コードを `master` ブランチにプッシュするだけで自動デプロイが実行されます！