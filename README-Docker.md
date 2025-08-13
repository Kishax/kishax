# Kishax Minecraft Server Docker Environment

このDockerfileとdocker-compose.ymlは、KishaxのMinecraftサーバー環境を簡単にセットアップするためのものです。

## 含まれる機能

- **MySQL 8.0サーバー**: docker/data/TABLES.sql のテーブル定義を自動インポート
- **Java 21環境**: 最新のJava環境で動作
- **Paper 1.21.8サーバー**: 最新のPaperサーバー
- **Velocity プロキシサーバー**: 最新のVelocityプロキシ
- **Kishaxプラグインのビルドとインストール**: SpigotとVelocity用プラグインを自動ビルド
- **LuckPermsプラグイン**: 権限管理プラグインを自動インストール
- **自動設定**: MySQL資格情報とforwarding secretの自動設定

## セットアップ手順

1. **環境変数ファイルの作成**:
   ```bash
   cp .env.example .env
   ```

2. **環境変数の編集** (.envファイル):
   ```env
   MYSQL_DATABASE=mc
   MYSQL_USER=mcuser
   MYSQL_PASSWORD=your_secure_password
   SPIGOT_MEMORY=2G
   VELOCITY_MEMORY=1G
   ```

3. **サーバーの起動**:
   ```bash
   docker-compose up -d
   ```

## ポート設定

- **25565**: Spigot (Paper) サーバーポート
- **25577**: Velocity プロキシポート  
- **3306**: MySQL データベースポート

## データの永続化

以下のデータはボリュームとして永続化されます：
- `mysql_data`: MySQLデータベースファイル
- `minecraft_data`: メインワールドデータ
- `minecraft_data_nether`, `minecraft_data_the_end`: 各次元のワールドデータ
- `velocity_data`: Velocityサーバーデータ
- `server_images`: サーバー内の画像ファイル

## 自動設定される項目

- **MySQL接続設定**: KishaxプラグインのMySQL設定が環境変数から自動設定
- **Velocity Forwarding Secret**: セキュアなランダム文字列が自動生成
- **データベーステーブル**: docker/data/TABLES.sql が自動実行

## 管理コマンド

```bash
# ログの確認
docker-compose logs -f kishax-server

# MySQLに接続
docker-compose exec mysql mysql -u root -p

# サーバーの停止
docker-compose down

# データも含めて完全削除（注意：データが失われます）
docker-compose down -v
```

## 注意事項

- 初回起動時にはプラグインのビルドとダウンロードが行われるため、時間がかかる場合があります
- MySQL接続が確立されるまでMinecraftサーバーは待機します
- Velocity forwarding secretは自動生成されるため、手動での設定は不要です