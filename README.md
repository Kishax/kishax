# KishaX

次世代認証システム搭載のモダンWebアプリケーション

[![Visit Website](https://img.shields.io/badge/Visit_Website-007BFF?style=for-the-badge)](https://kishax.net/)
[![Vulnerabilities](https://img.shields.io/badge/Vulnerabilities-0-success?style=for-the-badge)](https://github.com/your-username/kishax-nextjs)

## 🚀 クイックスタート

### 環境設定
```bash
git clone https://github.com/your-username/kishax-nextjs.git
cd kishax-nextjs
cp .env.example .env
# .envファイルを編集

# Playwright & Claude MCP セットアップ
npx playwright install chrome
claude mcp add playwright npx @playwright/mcp@latest
claude mcp add --transport sse deepwiki https://mcp.deepwiki.com/sse
```

### 開発方法を選択

#### 🔥 方法A: ホスト実行（推奨）
```bash
npm run docker:db    # PostgreSQLのみ起動
npm run db:push      # DBスキーマ反映
npm run dev          # 開発サーバー起動
```

#### 🐳 方法B: Docker実行
```bash
npm run docker:dev   # DB+APP同時起動
npm run db:push      # DBスキーマ反映
```

→ http://localhost:3000

## 🎯 主要機能

- **認証システム**: NextAuth.js v5 + OAuth (Google/Discord/Twitter)
- **データベース**: PostgreSQL + Prisma ORM
- **チャート**: Chart.js統合
- **Minecraft連携**: 独自認証システム
- **API仕様書**: OpenAPI 3.0 + Scalar UI
- **セキュリティ**: CSRF保護、XSS対策

## 📋 コマンド一覧

### 開発
```bash
npm run dev          # 開発サーバー
npm run build        # 本番ビルド
npm run lint         # コード検証
```

### Docker
```bash
npm run docker:dev   # 全サービス起動
npm run docker:db    # PostgreSQLのみ
npm run docker:down  # 停止
```

### データベース
```bash
npm run db:push      # スキーマ反映
npm run db:studio    # GUI管理画面
npm run db:migrate   # マイグレーション
```

## 🚀 本番デプロイ（AWS App Runner）

### 1. 事前準備
```bash
# RDS PostgreSQL作成
aws rds create-db-instance \
  --db-instance-identifier kishax-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username postgres \
  --master-user-password YOUR_PASSWORD \
  --allocated-storage 20 \
  --storage-encrypted true
```

### 2. Secrets Manager設定
```bash
aws secretsmanager create-secret \
  --name "kishax-apprunner-secrets" \
  --secret-string '{
    "NEXTAUTH_URL": "https://your-domain.awsapprunner.com",
    "NEXTAUTH_SECRET": "your-32-char-secret",
    "DATABASE_URL": "postgresql://user:pass@rds-endpoint:5432/db",
    "GOOGLE_CLIENT_ID": "your-google-id",
    "GOOGLE_CLIENT_SECRET": "your-google-secret"
  }' \
  --region ap-northeast-1
```

### 3. App Runnerサービス作成
- GitHub連携で自動デプロイ設定
- VPCコネクタでRDS接続
- Secrets Manager統合

### 4. デプロイ後設定
```bash
# データベース初期化
npx prisma migrate deploy

# OAuth リダイレクトURI更新
# Google: https://your-domain.awsapprunner.com/api/auth/callback/google
# Discord: https://your-domain.awsapprunner.com/api/auth/callback/discord
```

## 🏗️ 技術スタック

- **フレームワーク**: Next.js 15 + React 19
- **認証**: NextAuth.js v5
- **データベース**: PostgreSQL + Prisma
- **スタイリング**: Tailwind CSS v4
- **デプロイ**: AWS App Runner
- **コンテナ**: Docker Compose

## 💰 運用コスト

- App Runner: ~$7/月
- RDS PostgreSQL: ~$13/月
- VPCコネクタ: ~$7/月
- **合計**: ~$27/月

## 📚 API Documentation

開発環境: http://localhost:3000/docs  
本番環境: https://your-domain.awsapprunner.com/docs

## 🤝 コントリビューション

開発者・サポーター・アドバイザーを募集中！  
CSS/HTML、Node.jsサーバーコードに興味がある方は [お問い合わせ](https://maekawa.dev/contact) まで。

## 📄 ライセンス

[MIT](LICENSE)

---

Based on [mlgta](https://github.com/takayamaekawa/mlgta) - TypeScript版として最初に開発