# Kishax Infrastructure

Minecraft サーバー Kishax の統合インフラストラクチャプロジェクト

## プロジェクト構成

```
kishax/
├── aws/                    # AWS共通リソース・ポリシー
├── discord-bot/           # Discord Bot (ECS)
├── gather-bot/            # Gather Bot (ECS)
├── mc-plugins/            # Minecraft プラグイン (Velocity + Spigot)
├── web/                   # Web アプリケーション (ECS)
└── docs/                  # 共通ドキュメント
```

## サービス概要

### Discord Bot
Minecraft サーバーのイベントを Discord チャンネルに通知

- **技術スタック**: Java, Discord JDA
- **インフラ**: AWS ECS (Fargate)
- **通信**: SQS経由でメッセージ受信

### Gather Bot  
Slack 通知機能付きの Gather.town 監視ボット

- **技術スタック**: Node.js
- **インフラ**: AWS ECS (Fargate)
- **機能**: ユーザー状態監視、Slack通知

### Minecraft Plugins
Velocity Proxy と Spigot サーバー用のプラグイン

- **技術スタック**: Java, Velocity API, Spigot API
- **機能**: プレイヤー管理、AWS連携、テレポート、権限管理

### Web Application
プレイヤー認証・管理用Webアプリケーション

- **技術スタック**: Next.js, TypeScript, Prisma
- **インフラ**: AWS ECS (Fargate)
- **機能**: Discord/Google認証、OTP認証、プレイヤー管理

## クイックスタート

### 初回セットアップ
```bash
make setup-first-time
make setup-prerequisites
```

### デプロイ
```bash
# 全サービスデプロイ
make deploy-all

# 個別サービスデプロイ  
make deploy-discord-bot
make deploy-gather-bot
make deploy-web
```

### Minecraftプラグインビルド
```bash
make build-mc-plugins
```

## アーキテクチャ概要

```
[Minecraft Server] → [API Gateway] → [Lambda] → [SQS] → [Discord Bot]
[Web App] ←→ [RDS PostgreSQL]
[Gather Bot] → [Slack API]
```

## 開発ガイド

- [デプロイメントガイド](./aws/DEPLOY.md)
- [AWS インフラ詳細](./aws/README.md)
- [各サービス別README](./*/README.md)

## ライセンス

このプロジェクトは私的利用のために開発されています。
ただ、[MIT](LICENSE)ライセンスに基づき、コードの使用・改変は自由です。

---

**最終更新**: 2025-08-21
**管理者**: Kishax
