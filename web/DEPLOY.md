# KishaX - AWSデプロイメントガイド

このドキュメントではKishaXをAWSにデプロイする方法を説明します。2つのパターンを用意しており、セキュリティ要件に応じて選択してください。

## 📋 デプロイパターン比較

| パターン | セキュリティ | 複雑さ | コスト | 推奨用途 |
|----------|-------------|--------|--------|----------|
| **A. プライベート（VPCコネクタ）** | 高 | 高 | ~$34/月 | 本番環境 |
| **B. パブリック** | 中 | 低 | ~$20/月 | 開発・テスト環境 |

## 🔒 パターンA: プライベートデプロイ（VPCコネクタ使用）

### メリット
- RDSがプライベートネットワーク内で保護される
- インターネットからの直接アクセスを防げる
- 企業レベルのセキュリティ要件に対応

### デメリット
- 設定が複雑
- VPCコネクタの追加料金（$7/月）
- トラブルシューティングが困難

### 1. VPCとサブネットの準備

```bash
# VPC作成
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --region ap-northeast-1 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=kishax-vpc}]'

# プライベートサブネット作成（AZ-1a）
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.1.0/24 \
  --availability-zone ap-northeast-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-private-1a}]'

# プライベートサブネット作成（AZ-1c）
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.2.0/24 \
  --availability-zone ap-northeast-1c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=kishax-private-1c}]'
```

### 2. セキュリティグループ作成

```bash
# App Runner用セキュリティグループ
aws ec2 create-security-group \
  --group-name kishax-apprunner-sg \
  --description "Security group for App Runner VPC connector" \
  --vpc-id vpc-xxxxx

# RDS用セキュリティグループ
aws ec2 create-security-group \
  --group-name kishax-rds-sg \
  --description "Security group for RDS PostgreSQL" \
  --vpc-id vpc-xxxxx

# App RunnerからRDSへのアクセス許可
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-apprunner-xxxxx

# App Runnerのアウトバウンドルール
aws ec2 authorize-security-group-egress \
  --group-id sg-apprunner-xxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-egress \
  --group-id sg-apprunner-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-rds-xxxxx
```

### 3. VPCコネクタ作成

```bash
aws apprunner create-vpc-connector \
  --vpc-connector-name kishax-vpc-connector \
  --subnets subnet-xxxxx subnet-yyyyy \
  --security-groups sg-apprunner-xxxxx \
  --region ap-northeast-1
```

### 4. RDS作成（プライベート）

```bash
# DBサブネットグループ作成
aws rds create-db-subnet-group \
  --db-subnet-group-name kishax-db-subnet-group \
  --db-subnet-group-description "Subnet group for KishaX RDS" \
  --subnet-ids subnet-xxxxx subnet-yyyyy

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

### 5. IAMロール作成

```bash
# App Runner Instance Role
aws iam create-role \
  --role-name AppRunnerInstanceRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "tasks.apprunner.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

# Secrets Manager権限付与
aws iam attach-role-policy \
  --role-name AppRunnerInstanceRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

### 6. Secrets Manager設定

```bash
aws secretsmanager create-secret \
  --name "kishax-apprunner-secrets" \
  --secret-string '{
    "NEXTAUTH_URL": "https://your-domain.awsapprunner.com",
    "NEXTAUTH_SECRET": "your-32-char-secret-here",
    "DATABASE_URL": "postgresql://postgres:YOUR_PASSWORD@kishax-postgres.xxxxx.ap-northeast-1.rds.amazonaws.com:5432/postgres",
    "GOOGLE_CLIENT_ID": "your-google-client-id",
    "GOOGLE_CLIENT_SECRET": "your-google-client-secret",
    "DISCORD_CLIENT_ID": "your-discord-client-id",
    "DISCORD_CLIENT_SECRET": "your-discord-client-secret"
  }' \
  --region ap-northeast-1
```

### 7. App Runnerサービス作成

```bash
# apprunner.yamlを確認（プロジェクトルートに配置）
cat > apprunner.yaml << 'EOF'
version: 1.0
runtime: nodejs18
build:
  commands:
    build:
      - echo "Installing dependencies"
      - npm ci --only=production
      - echo "Building application"
      - npm run build
run:
  runtime-version: 18
  command: npm start
  network:
    port: 3000
    env:
      - name: PORT
        value: "3000"
  env:
    - name: NODE_ENV
      value: production
EOF

# App Runnerサービス作成（VPCコネクタ付き）
aws apprunner create-service \
  --service-name kishax-app \
  --source-configuration '{
    "ImageRepository": null,
    "CodeRepository": {
      "RepositoryUrl": "https://github.com/your-username/kishax-nextjs",
      "SourceCodeVersion": {
        "Type": "BRANCH",
        "Value": "master"
      },
      "CodeConfiguration": {
        "ConfigurationSource": "REPOSITORY"
      }
    },
    "AutoDeploymentsEnabled": true
  }' \
  --instance-configuration '{
    "Cpu": "1 vCPU",
    "Memory": "2 GB",
    "InstanceRoleArn": "arn:aws:iam::YOUR_ACCOUNT:role/AppRunnerInstanceRole"
  }' \
  --network-configuration '{
    "EgressConfiguration": {
      "EgressType": "VPC",
      "VpcConnectorArn": "arn:aws:apprunner:ap-northeast-1:YOUR_ACCOUNT:vpcconnector/kishax-vpc-connector"
    }
  }'
```

---

## 🌐 パターンB: パブリックデプロイ（シンプル）

### メリット
- 設定が簡単
- 低コスト
- トラブルシューティングが容易

### デメリット
- RDSがインターネットに露出
- セキュリティグループでの保護が必要
- 企業環境では推奨されない

### 1. RDS作成（パブリックアクセス有効）

```bash
# RDS作成（パブリック）
aws rds create-db-instance \
  --db-instance-identifier kishax-postgres-public \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.7 \
  --master-username postgres \
  --master-user-password "YOUR_SECURE_PASSWORD" \
  --allocated-storage 20 \
  --storage-encrypted true \
  --publicly-accessible \
  --backup-retention-period 7 \
  --deletion-protection
```

### 2. セキュリティグループ設定（パブリック）

```bash
# RDS用セキュリティグループ（より厳格）
aws ec2 create-security-group \
  --group-name kishax-rds-public-sg \
  --description "Security group for public RDS PostgreSQL"

# App Runnerの固定IP範囲からのアクセス許可
# 注意: App Runnerの実際のIP範囲を確認して設定
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds-public-xxxxx \
  --protocol tcp \
  --port 5432 \
  --cidr 3.112.23.0/29  # App Runner ap-northeast-1の例

# 必要に応じて管理用IP追加
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds-public-xxxxx \
  --protocol tcp \
  --port 5432 \
  --cidr YOUR_ADMIN_IP/32
```

### 3. IAMロール作成（同じ）

```bash
# App Runner Instance Role（パターンAと同じ）
aws iam create-role \
  --role-name AppRunnerInstanceRolePublic \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "tasks.apprunner.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

aws iam attach-role-policy \
  --role-name AppRunnerInstanceRolePublic \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

### 4. Secrets Manager設定（パブリック）

```bash
aws secretsmanager create-secret \
  --name "kishax-apprunner-secrets-public" \
  --secret-string '{
    "NEXTAUTH_URL": "https://your-domain.awsapprunner.com",
    "NEXTAUTH_SECRET": "your-32-char-secret-here",
    "DATABASE_URL": "postgresql://postgres:YOUR_PASSWORD@kishax-postgres-public.xxxxx.ap-northeast-1.rds.amazonaws.com:5432/postgres",
    "GOOGLE_CLIENT_ID": "your-google-client-id",
    "GOOGLE_CLIENT_SECRET": "your-google-client-secret",
    "DISCORD_CLIENT_ID": "your-discord-client-id",
    "DISCORD_CLIENT_SECRET": "your-discord-client-secret"
  }' \
  --region ap-northeast-1
```

### 5. App Runnerサービス作成（VPCコネクタなし）

```bash
# App Runnerサービス作成（パブリック）
aws apprunner create-service \
  --service-name kishax-app-public \
  --source-configuration '{
    "ImageRepository": null,
    "CodeRepository": {
      "RepositoryUrl": "https://github.com/your-username/kishax-nextjs",
      "SourceCodeVersion": {
        "Type": "BRANCH",
        "Value": "master"
      },
      "CodeConfiguration": {
        "ConfigurationSource": "REPOSITORY"
      }
    },
    "AutoDeploymentsEnabled": true
  }' \
  --instance-configuration '{
    "Cpu": "1 vCPU",
    "Memory": "2 GB",
    "InstanceRoleArn": "arn:aws:iam::YOUR_ACCOUNT:role/AppRunnerInstanceRolePublic"
  }'
```

---

## 🚀 共通デプロイ手順

### 1. データベース初期化

```bash
# App Runnerコンソールで一度だけ実行
npx prisma migrate deploy
npx prisma db seed  # seedがあれば
```

### 2. OAuth設定更新

デプロイ完了後、各OAuthプロバイダーのリダイレクトURIを更新：

- **Google**: https://your-domain.awsapprunner.com/api/auth/callback/google
- **Discord**: https://your-domain.awsapprunner.com/api/auth/callback/discord

### 3. カスタムドメイン設定（オプション）

```bash
# Certificate Manager証明書作成
aws acm request-certificate \
  --domain-name yourdomain.com \
  --validation-method DNS \
  --region ap-northeast-1

# App Runnerにカスタムドメイン関連付け
aws apprunner associate-custom-domain \
  --service-arn arn:aws:apprunner:ap-northeast-1:account:service/kishax-app \
  --domain-name yourdomain.com
```

---

## 💰 運用コスト詳細

### パターンA（プライベート）
- App Runner: ~$7/月
- RDS t3.micro: ~$13/月
- VPCコネクタ: ~$7/月
- データ転送: ~$5/月
- **合計**: ~$32/月

### パターンB（パブリック）
- App Runner: ~$7/月
- RDS t3.micro: ~$13/月
- **合計**: ~$20/月

---

## 🔧 トラブルシューティング

### 接続エラー

**症状**: データベースに接続できない

**対処法**:
```bash
# セキュリティグループ確認
aws ec2 describe-security-groups --group-ids sg-xxxxx

# RDSエンドポイント確認
aws rds describe-db-instances --db-instance-identifier kishax-postgres

# VPCコネクタステータス確認（パターンAのみ）
aws apprunner describe-vpc-connector --vpc-connector-arn arn:aws:apprunner:...
```

### DNS解決エラー

**症状**: RDSエンドポイントが解決できない

**対処法**:
```bash
# VPCのDNS設定確認
aws ec2 describe-vpcs --vpc-ids vpc-xxxxx

# DNS解決とホスト名解決を有効化
aws ec2 modify-vpc-attribute --vpc-id vpc-xxxxx --enable-dns-support
aws ec2 modify-vpc-attribute --vpc-id vpc-xxxxx --enable-dns-hostnames
```

### アプリケーションタイムアウト

**症状**: App Runnerでタイムアウトエラー

**対処法**:
```bash
# App Runnerログ確認
aws logs filter-log-events \
  --log-group-name /aws/apprunner/kishax-app \
  --start-time 1640995200000

# インスタンス設定見直し
aws apprunner update-service \
  --service-arn arn:aws:apprunner:... \
  --instance-configuration '{"Cpu":"1 vCPU","Memory":"3 GB"}'
```

### Secrets Manager接続エラー

**症状**: 環境変数が読み込めない

**対処法**:
```bash
# IAMロール権限確認
aws iam list-attached-role-policies --role-name AppRunnerInstanceRole

# Secrets Manager値確認
aws secretsmanager get-secret-value --secret-id kishax-apprunner-secrets
```

---

## 🛡️ セキュリティベストプラクティス

### 1. 最小権限の原則
- セキュリティグループは必要最小限のポートのみ開放
- IAMロールは必要最小限の権限のみ付与

### 2. 定期的な更新
```bash
# RDSメンテナンスウィンドウ設定
aws rds modify-db-instance \
  --db-instance-identifier kishax-postgres \
  --preferred-maintenance-window "sun:18:00-sun:19:00"

# Auto Minor Version Upgrade有効化
aws rds modify-db-instance \
  --db-instance-identifier kishax-postgres \
  --auto-minor-version-upgrade
```

### 3. バックアップ設定
```bash
# 自動バックアップ設定
aws rds modify-db-instance \
  --db-instance-identifier kishax-postgres \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00"
```

### 4. 監視設定
```bash
# CloudWatch Logs設定
aws rds modify-db-instance \
  --db-instance-identifier kishax-postgres \
  --cloudwatch-logs-configuration '{"LogTypes":["postgresql"],"Enable":true}'
```

---

## 📚 参考リンク

- [AWS App Runner開発者ガイド](https://docs.aws.amazon.com/apprunner/)
- [Amazon RDS PostgreSQL](https://docs.aws.amazon.com/rds/latest/userguide/CHAP_PostgreSQL.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Next.js デプロイメントガイド](https://nextjs.org/docs/deployment)