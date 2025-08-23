# Kishax Infrastructure

Minecraft サーバー Kishax の統合インフラストラクチャプロジェクト

## プロジェクト構成

```
kishax/
├── apps/                   # アプリケーション層
│   ├── discord-bot/       # Discord Bot (ECS)
│   ├── gather-bot/        # Gather Bot (ECS)
│   ├── mc-plugins/        # Minecraft プラグイン (Velocity + Spigot)
│   └── web/               # Web アプリケーション (ECS)
├── aws/                   # AWS共通リソース・ポリシー
└── docs/                  # 共通ドキュメント
```

## サービス概要

このプロジェクトは4つの主要アプリケーションとAWSインフラから構成されています：

- **Discord Bot**: MinecraftイベントのDiscord通知 (Java/ECS)
- **Gather Bot**: Gather.town監視とSlack通知 (Node.js/ECS)  
- **Minecraft Plugins**: Velocity/Spigotプラグイン (Java)
- **Web Application**: プレイヤー認証・管理 (Next.js/ECS)

📁 **詳細**: [apps/README.md](./apps/README.md)

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
# プラグイン個別ビルドは apps/mc-plugins/ で実行
cd apps/mc-plugins
make build-all
```

## アーキテクチャ概要

```
[Minecraft Server] → [API Gateway] → [Lambda] → [SQS] → [Discord Bot]
[Web App] ←→ [RDS PostgreSQL]
[Gather Bot] → [Slack API]
```

## 開発ガイド

- **アプリケーション**: [apps/README.md](./apps/README.md)
- **AWS インフラ**: [aws/README.md](./aws/README.md)  
- **デプロイメント**: [aws/DEPLOY.md](./aws/DEPLOY.md)
- **各サービス詳細**: [apps/*/README.md](./apps/)

## ライセンス

このプロジェクトは私的利用のために開発されています。
ただ、[MIT](LICENSE)ライセンスに基づき、コードの使用・改変は自由です。

---

**最終更新**: 2025-08-21
**管理者**: Kishax
