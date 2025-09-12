# Kishax Minecraft Server Development Environment

## QuickStart

```bash
cp .env.example .env

cd docker/data/
cp velocity-kishax-config.yml.example velocity-kishax-config.yml
cp spigot-kishax-config.yml.example spigot-kishax-config.yml

docker-compose up -d
```

## シードデータの挿入方法
開発環境でMySQLデータベースにシードデータを挿入したい場合は、以下の手順で行えます：

1. `docker/data/seeds/` ディレクトリにSQLファイルを配置
2. Dockerコンテナを再起動

**例：**
```sql
-- docker/data/seeds/user_seeds.sql
INSERT INTO users (name, email) VALUES 
    ('テストユーザー', 'test@example.com'),
    ('管理者', 'admin@example.com');
```

**注意事項：**
- `docker/data/seeds/example.sql` はサンプルファイルです
- 実際のシードファイルは `.gitignore` により Git 管理対象外となります
- MySQLコンテナ初期化時に自動的に読み込まれます

## 本番環境で忘れずに行うこと
- 環境変数のQUEUE\_MODEをMCにする
