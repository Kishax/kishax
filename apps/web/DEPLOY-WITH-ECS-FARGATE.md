# KishaX - ECS Fargateデプロイメントガイド

このドキュメントではKishaXをAWS ECS Fargateにデプロイする方法を説明します。
AppRunnerより高度な制御とカスタマイズが可能で、企業レベルの本格的なデプロイメントに適しています。

## 📋 ECS Fargate vs AppRunner比較

| 項目 | ECS Fargate | AppRunner |
|------|-------------|-----------|
| **制御レベル** | 高（詳細設定可能） | 低（シンプル） |
| **ロードバランサー** | ALB/NLB対応 | 内蔵のみ |
| **SSL/TLS** | 証明書完全制御 | 自動証明書のみ |
| **ネットワーク** | VPC完全制御 | VPCコネクタのみ |
| **監視** | CloudWatch完全対応 | 基本監視のみ |
| **コスト** | $25-35/月 | $20-34/月 |
| **複雑さ** | 高 | 低 |

## 🏗️ アーキテクチャ概要

```
Internet Gateway
    ↓
Application Load Balancer (ALB)
    ↓
ECS Fargate Service (Auto Scaling)
    ↓
RDS PostgreSQL (プライベートサブネット)
```

## 🚀 デプロイメント手順

### 1. 前提条件

```bash
# AWS CLI設定確認
aws sts get-caller-identity

# 必要なツール
- AWS CLI v2
- Docker
- Node.js 18+
- GitHub Actions権限
```

### 2. VPCとネットワーク設定

```bash
# VPC作成
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --region $(AWS_REGION) \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=kishax-vpc}]'

# Internet Gateway作成・アタッチ
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=kishax-igw}]'

aws ec2 attach-internet-gateway \
  --internet-gateway-id igw-xxxxx \
  --vpc-id vpc-xxxxx

# パブリックサブネット作成（ALB用）
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.1.0/24 \
  --availability-zone $(AWS_REGION)a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-public-1a}]'

aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.2.0/24 \
  --availability-zone $(AWS_REGION)c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-public-1c}]'

# プライベートサブネット作成（ECS/RDS用）
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.3.0/24 \
  --availability-zone $(AWS_REGION)a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-private-1a}]'

aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.4.0/24 \
  --availability-zone $(AWS_REGION)c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-private-1c}]'
```

### 3. セキュリティグループ作成

```bash
# ALB用セキュリティグループ
aws ec2 create-security-group \
  --group-name kishax-alb-sg \
  --description "Security group for ALB" \
  --vpc-id vpc-xxxxx

# HTTP/HTTPS許可
aws ec2 authorize-security-group-ingress \
  --group-id sg-alb-xxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id sg-alb-xxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# ECS用セキュリティグループ
aws ec2 create-security-group \
  --group-name kishax-ecs-sg \
  --description "Security group for ECS tasks" \
  --vpc-id vpc-xxxxx

# ALBからECSへの通信許可
aws ec2 authorize-security-group-ingress \
  --group-id sg-ecs-xxxxx \
  --protocol tcp \
  --port 3000 \
  --source-group sg-alb-xxxxx

# RDS用セキュリティグループ
aws ec2 create-security-group \
  --group-name kishax-rds-sg \
  --description "Security group for RDS" \
  --vpc-id vpc-xxxxx

# ECSからRDSへの通信許可
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-ecs-xxxxx
```

### 4. RDS作成

```bash
# DBサブネットグループ作成
aws rds create-db-subnet-group \
  --db-subnet-group-name kishax-db-subnet-group \
  --db-subnet-group-description "Subnet group for KishaX RDS" \
  --subnet-ids subnet-private-1a subnet-private-1c

# RDS PostgreSQL作成
aws rds create-db-instance \
  --db-instance-identifier kishax-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.7 \
  --master-username postgres \
  --master-user-password "YOUR_SECURE_PASSWORD" \
  --allocated-storage 20 \
  --storage-encrypted true \
  --vpc-security-group-ids sg-rds-xxxxx \
  --db-subnet-group-name kishax-db-subnet-group \
  --no-publicly-accessible \
  --backup-retention-period 7 \
  --deletion-protection
```

### 5. SSL証明書取得

```bash
# ACM証明書要求
aws acm request-certificate \
  --domain-name kishax.net \
  --subject-alternative-names "*.kishax.net" \
  --validation-method DNS \
  --region $(AWS_REGION)

# DNS検証レコードをRoute 53に追加（証明書ARNで確認）
aws acm describe-certificate --certificate-arn arn:aws:acm:...
```

### 6. Application Load Balancer作成

```bash
# ALB作成
aws elbv2 create-load-balancer \
  --name kishax-alb \
  --subnets subnet-public-1a subnet-public-1c \
  --security-groups sg-alb-xxxxx \
  --scheme internet-facing

# ターゲットグループ作成
aws elbv2 create-target-group \
  --name kishax-tg \
  --protocol HTTP \
  --port 3000 \
  --vpc-id vpc-xxxxx \
  --target-type ip \
  --health-check-path "/api/health" \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3

# HTTPSリスナー作成
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:... \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...

# HTTPリダイレクトリスナー作成
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=redirect,RedirectConfig='{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

### 7. IAMロール作成

```bash
# ECSタスク実行ロール
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "ecs-tasks.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# ECSタスクロール（アプリケーション用）
aws iam create-role \
  --role-name AppRunnerInstanceRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": ["tasks.apprunner.amazonaws.com", "ecs-tasks.amazonaws.com"]
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

aws iam attach-role-policy \
  --role-name AppRunnerInstanceRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

### 8. ECRリポジトリ作成

```bash
# ECRリポジトリ作成
aws ecr create-repository \
  --repository-name kishax-web \
  --region $(AWS_REGION)

# Docker認証
aws ecr get-login-password --region $(AWS_REGION) | \
  docker login --username AWS --password-stdin $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com
```

### 9. ECSクラスター作成

```bash
# ECSクラスター作成
aws ecs create-cluster \
  --cluster-name kishax-cluster \
  --capacity-providers FARGATE \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
```

### 10. Secrets Manager設定

```bash
# 環境変数設定
aws secretsmanager create-secret \
  --name "kishax-apprunner-secrets" \
  --secret-string '{
    "NEXTAUTH_URL": "https://kishax.net",
    "NEXTAUTH_SECRET": "your-32-char-secret-here",
    "DATABASE_URL": "postgresql://postgres:YOUR_PASSWORD@kishax-postgres.xxxxx.$(AWS_REGION).rds.amazonaws.com:5432/postgres",
    "GOOGLE_CLIENT_ID": "your-google-client-id",
    "GOOGLE_CLIENT_SECRET": "your-google-client-secret",
    "DISCORD_CLIENT_ID": "your-discord-client-id",
    "DISCORD_CLIENT_SECRET": "your-discord-client-secret",
    "TWITTER_CLIENT_ID": "your-twitter-client-id",
    "TWITTER_CLIENT_SECRET": "your-twitter-client-secret",
    "EMAIL_HOST": "email-smtp.$(AWS_REGION).amazonaws.com",
    "EMAIL_PORT": "587",
    "EMAIL_USER": "your-ses-smtp-username",
    "EMAIL_PASS": "your-ses-smtp-password",
    "EMAIL_FROM": "noreply@kishax.net"
  }' \
  --region $(AWS_REGION)
```

### 11. CloudWatch Logs設定

```bash
# ログループ作成
aws logs create-log-group \
  --log-group-name "/ecs/kishax-web" \
  --region $(AWS_REGION)
```

### 12. ECSタスク定義とサービス作成

ファイル配置:
- `aws/task-definition.json` - 実際の設定
- `aws/task-definition.json.example` - テンプレート例

```bash
# タスク定義登録
aws ecs register-task-definition \
  --cli-input-json file://aws/task-definition.json

# ECSサービス作成
aws ecs create-service \
  --cluster kishax-cluster \
  --service-name kishax-web-service \
  --task-definition kishax-web-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration '{
    "awsvpcConfiguration": {
      "subnets": ["subnet-private-1a", "subnet-private-1c"],
      "securityGroups": ["sg-ecs-xxxxx"],
      "assignPublicIp": "ENABLED"
    }
  }' \
  --load-balancers '[{
    "targetGroupArn": "arn:aws:elasticloadbalancing:...",
    "containerName": "kishax-web",
    "containerPort": 3000
  }]'
```

### 13. Route 53 DNS設定

```bash
# ALBのDNS名を確認
aws elbv2 describe-load-balancers --names kishax-alb

# Route 53レコード作成
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "kishax.net",
        "Type": "A",
        "AliasTarget": {
          "DNSName": "kishax-alb-xxxxx.$(AWS_REGION).elb.amazonaws.com",
          "EvaluateTargetHealth": true,
          "HostedZoneId": "Z14GRHDCWA56QT"
        }
      }
    }]
  }'
```

## 🤖 GitHub Actions CI/CD設定

### 1. GitHub Actions用IAMユーザー作成

```bash
# IAMユーザー作成
aws iam create-user --user-name github-actions-deploy

# 最小権限ポリシー作成（aws/github-actions-policy.json参照）
aws iam create-policy \
  --policy-name GitHubActionsDeployPolicy \
  --policy-document file://aws/github-actions-policy.json

# ポリシーアタッチ
aws iam attach-user-policy \
  --user-name github-actions-deploy \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT:policy/GitHubActionsDeployPolicy

# アクセスキー作成
aws iam create-access-key --user-name github-actions-deploy
```

### 2. GitHub Secrets設定

GitHub リポジトリの Settings > Secrets で以下を設定:

```
AWS_ACCESS_KEY_ID: $(AWS_ACCESS_KEY_ID_FOR_GITHUB_ACTIONS)
AWS_SECRET_ACCESS_KEY: $(AWS_SECRET_ACCESS_KEY_FOR_GITHUB_ACTIONS)
SLACK_WEBHOOK_URL: https://hooks.slack.com/services/... (オプション)
```

> 💡 **重要**: これらの資格情報は最小権限の専用IAMユーザー用です。本番環境では絶対に管理者権限を使用しないでください。

### 3. GitHub Actionsワークフロー

`.github/workflows/deploy.yml` が自動デプロイを処理します:

- masterブランチへのプッシュで自動デプロイ
- Docker イメージビルド・ECRプッシュ
- ECS サービス更新
- Slack通知（オプション）

## 🔧 運用・メンテナンス

### ヘルスチェック確認

```bash
# ECSサービス状態確認
aws ecs describe-services \
  --cluster kishax-cluster \
  --services kishax-web-service

# ALBターゲット状態確認
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:...

# アプリケーションヘルスチェック
curl https://kishax.net/api/health
```

### ログ確認

```bash
# ECS コンテナログ
aws logs filter-log-events \
  --log-group-name "/ecs/kishax-web" \
  --start-time $(date -d '10 minutes ago' +%s)000

# ALB アクセスログ
aws logs filter-log-events \
  --log-group-name "/aws/applicationloadbalancer/kishax-alb"
```

### スケーリング設定

```bash
# Auto Scaling設定
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/kishax-cluster/kishax-web-service \
  --min-capacity 1 \
  --max-capacity 10

# CPU使用率ベースのスケーリングポリシー
aws application-autoscaling put-scaling-policy \
  --policy-name kishax-cpu-scaling \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/kishax-cluster/kishax-web-service \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    }
  }'
```

## 💰 コスト最適化

### 月額コスト詳細（$(AWS_REGION)）

- **ECS Fargate**: 1 vCPU, 2GB RAM × 24時間 ≈ $25/月
- **RDS t3.micro**: 20GB ストレージ付き ≈ $13/月  
- **ALB**: ≈ $16/月
- **データ転送**: ≈ $2/月
- **Route 53**: $0.5/月
- **合計**: **約 $57/月**

### コスト削減案

1. **Spot Fargate**: 最大70%削減（本番環境非推奨）
2. **Reserved Instances**: RDSで最大60%削減
3. **CloudWatch Logs保持期間**: 7日で十分

## 🛡️ セキュリティベストプラクティス

### 1. ネットワークセキュリティ

- ECSタスクはプライベートサブネット配置
- セキュリティグループで最小権限アクセス
- RDSはパブリックアクセス無効

### 2. アクセス制御

- IAMロール最小権限の原則
- Secrets Manager使用（平文環境変数禁止）
- GitHub Actions専用IAMユーザー

### 3. 監査・監視

```bash
# CloudTrail有効化
aws cloudtrail create-trail \
  --name kishax-audit-trail \
  --s3-bucket-name kishax-cloudtrail-logs

# GuardDuty有効化  
aws guardduty create-detector \
  --enable \
  --finding-publishing-frequency FIFTEEN_MINUTES
```

## 🚨 トラブルシューティング

### ECSタスク起動失敗

```bash
# タスク停止理由確認
aws ecs describe-tasks \
  --cluster kishax-cluster \
  --tasks arn:aws:ecs:...

# CloudWatch Logs確認
aws logs filter-log-events \
  --log-group-name "/ecs/kishax-web" \
  --filter-pattern "ERROR"
```

### ALB ヘルスチェック失敗

```bash
# ターゲット詳細確認
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancer:...

# セキュリティグループ確認
aws ec2 describe-security-groups --group-ids sg-xxxxx
```

### データベース接続エラー

```bash
# RDS接続テスト（踏み台サーバーから）
psql -h kishax-postgres.xxxxx.$(AWS_REGION).rds.amazonaws.com \
     -U postgres -d postgres

# Secrets Manager値確認
aws secretsmanager get-secret-value \
  --secret-id kishax-apprunner-secrets
```

## 📁 ファイル構成

```
web/
├── aws/
│   ├── task-definition.json              # 実際のタスク定義
│   ├── task-definition.json.example      # テンプレート例
│   ├── github-actions-policy.json        # 実際のIAMポリシー
│   └── github-actions-policy.json.example # テンプレート例
├── .github/workflows/
│   └── deploy.yml                        # GitHub Actions設定
├── src/app/api/health/
│   └── route.ts                          # ヘルスチェックエンドポイント
└── PROJECT.md                            # 移行手順詳細
```

## 🔄 AppRunnerからの移行

既存のAppRunnerからECS Fargateへの移行については `PROJECT.md` を参照してください。

## 📚 参考リンク

- [Amazon ECS開発者ガイド](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate料金](https://aws.amazon.com/fargate/pricing/)
- [Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)
- [AWS Certificate Manager](https://docs.aws.amazon.com/acm/)
- [Amazon Route 53](https://docs.aws.amazon.com/route53/)