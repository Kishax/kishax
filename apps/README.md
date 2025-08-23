# Kishax Applications

Kishax プロジェクトのアプリケーション層。4つの独立したサービスから構成されています。

## アプリケーション構成

```
apps/
├── discord-bot/           # Discord Bot (ECS)
├── gather-bot/            # Gather Bot (ECS)
├── mc-plugins/            # Minecraft プラグイン (Velocity + Spigot)
└── web/                   # Web アプリケーション (ECS)
```

## サービス詳細

### Discord Bot

Minecraft サーバーのイベントを Discord チャンネルに通知するボット

- **技術スタック**: Java, Discord JDA
- **インフラ**: AWS ECS (Fargate)
- **通信**: SQS経由でメッセージ受信
- **機能**: 
  - プレイヤーの参加/退出通知
  - サーバー状態通知
  - 管理コマンド

**関連ファイル**: [discord-bot/README.md](./discord-bot/README.md)

### Gather Bot

Slack 通知機能付きの Gather.town 監視ボット

- **技術スタック**: Node.js
- **インフラ**: AWS ECS (Fargate)
- **機能**: 
  - ユーザー状態監視
  - Slack通知
  - 接続状態管理

**関連ファイル**: [gather-bot/README.md](./gather-bot/README.md)

### Minecraft Plugins

Velocity Proxy と Spigot サーバー用のプラグイン

- **技術スタック**: Java, Velocity API, Spigot API
- **機能**: 
  - プレイヤー管理
  - AWS連携
  - テレポート機能
  - 権限管理
  - ソケット通信

**関連ファイル**: [mc-plugins/README.md](./mc-plugins/README.md)

### Web Application

プレイヤー認証・管理用Webアプリケーション

- **技術スタック**: Next.js, TypeScript, Prisma
- **インフラ**: AWS ECS (Fargate)
- **機能**: 
  - Discord/Google認証
  - OTP認証
  - プレイヤー管理
  - Minecraft連携認証

**関連ファイル**: [web/README.md](./web/README.md)

## デプロイ方法

### 全サービス一括デプロイ

```bash
# ルートディレクトリから実行
make deploy-all
```

### 個別サービスデプロイ

```bash
# Discord Bot
make deploy-discord-bot

# Gather Bot
make deploy-gather-bot

# Web Application
make deploy-web

# Minecraft Plugins（手動デプロイ）
cd apps/mc-plugins
make build-all
```

## 開発環境セットアップ

### 前提条件

```bash
# 前提条件チェック
make setup-prerequisites
```

必要なツール:
- Java 17+
- Node.js 18+
- Docker
- AWS CLI

### 個別サービス開発

各サービスのディレクトリで個別に開発環境を構築できます：

```bash
# Discord Bot (Java)
cd apps/discord-bot
./gradlew build

# Gather Bot (Node.js)
cd apps/gather-bot
npm install
npm start

# Web Application (Next.js)
cd apps/web
npm install
npm run dev

# Minecraft Plugins (Java)
cd apps/mc-plugins
./gradlew build
```

## アーキテクチャ概要

```
[Minecraft Server] → [API Gateway] → [Lambda] → [SQS] → [Discord Bot]
[Web App] ←→ [RDS PostgreSQL]
[Gather Bot] → [Slack API]
[Minecraft Plugins] ←→ [Web App API]
```

## 共通ライブラリ・設定

各アプリケーション間で共有される設定やライブラリ:

- AWS設定: 全サービスで共通のIAMロール・ポリシーを使用
- データベース: WebアプリとMinecraftプラグインでPostgreSQLを共有
- 認証: 統一されたOAuth・OTP認証システム

## 監視・ログ

```bash
# 各サービスのログ確認
make logs-discord-bot
make logs-gather-bot
make logs-web

# サービス状態確認
make status-services
make status-ecs
```

## テスト

```bash
# 統合テスト
make test-integration

# Minecraft連携テスト
make test-minecraft-discord
make test-mc-plugins-integration
```

---

**注意**: 各サービスの詳細な設定方法は、各ディレクトリ内のREADME.mdを参照してください。