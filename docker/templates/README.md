# Minecraft Server Templates

このディレクトリには、Minecraftサーバーの設定ファイルテンプレートが格納されています。

## 📂 ディレクトリ構造

```
templates/
├── spigot/
│   ├── common/                    # 全サーバー共通の設定
│   │   ├── config/
│   │   │   └── paper-global.yml  # Paper共通設定
│   │   ├── plugins/
│   │   │   ├── Kishax/
│   │   │   │   └── config.yml    # Kishaxプラグイン共通設定
│   │   │   └── LuckPerms/
│   │   │       └── config.yml    # LuckPerms共通設定
│   │   └── server.properties      # サーバー共通プロパティ
│   │
│   └── server-specific/           # サーバー別の設定
│       ├── home/                  # homeサーバー専用
│       │   └── plugins/
│       │       └── Kishax/
│       │           └── portals.yml
│       │
│       └── latest/                # latestサーバー専用
│           └── plugins/
│               └── Kishax/
│                   └── portals.yml
│
└── velocity/
    └── plugins/
        ├── kishax/
        └── luckperms/
```

## 🔧 動作の仕組み

### 1. ビルド時 (Dockerfile)

```dockerfile
COPY docker/templates/spigot /mc/templates/spigot
COPY docker/templates/velocity /mc/templates/velocity
```

ビルド時に全テンプレートがDockerイメージに含まれます。

### 2. 起動時 (start.sh)

#### ステップ1: 共通ファイルのコピー

全サーバーに共通の設定ファイルをコピーします。

```bash
# common/ 以下のファイルを /mc/spigot/ にコピー
cp -r /mc/templates/spigot/common/* /mc/spigot/
```

#### ステップ2: サーバー別ファイルのコピー

各サーバー起動時に、サーバー名に対応するディレクトリが存在する場合、そのファイルを上書きコピーします。

```bash
# server-specific/{サーバー名}/ 以下のファイルを /mc/spigot/{サーバー名}/ にコピー
SERVER_SPECIFIC_DIR="/mc/templates/spigot/server-specific/$SPIGOT_NAME"
if [ -d "$SERVER_SPECIFIC_DIR" ]; then
    cp -r "$SERVER_SPECIFIC_DIR/plugins/"* "/mc/spigot/$SPIGOT_NAME/plugins/"
fi
```

**優先順位:**
1. `common/` - 全サーバー共通設定（ベース）
2. `server-specific/{サーバー名}/` - サーバー別設定（上書き）

## 📝 使用方法

### サーバー特有のファイルを追加する

1. **サーバー名を確認**
   - `servers.json` で定義されているサーバー名（例: `home`, `latest`）

2. **ディレクトリを作成**
   ```bash
   mkdir -p apps/mc/docker/templates/spigot/server-specific/<サーバー名>/plugins/Kishax
   ```

3. **設定ファイルを配置**
   ```bash
   # 例: homeサーバー専用の portals.yml を配置
   vim apps/mc/docker/templates/spigot/server-specific/home/plugins/Kishax/portals.yml
   ```

4. **Dockerイメージを再ビルド**
   ```bash
   cd apps/mc
   docker compose build
   ```

5. **コンテナを起動**
   ```bash
   docker compose up -d
   ```

### 全サーバー共通の設定を変更する

1. **共通設定ファイルを編集**
   ```bash
   vim apps/mc/docker/templates/spigot/common/plugins/Kishax/config.yml
   ```

2. **Dockerイメージを再ビルド**
   ```bash
   docker compose build && docker compose up -d
   ```

## 🔍 動作確認

### ログで確認

コンテナ起動時のログで、以下のメッセージが表示されます:

```
📁 Copying server-specific files for home...
  ✅ Copied server-specific plugin configs
```

または、サーバー特有ファイルがない場合:

```
ℹ️  No server-specific files for home (using common configs only)
```

### ファイル配置を確認

```bash
# コンテナ内部に入る
docker exec -it kishax-minecraft bash

# homeサーバーの設定を確認
ls -la /mc/spigot/home/plugins/Kishax/
# → config.yml (common) と portals.yml (server-specific) が両方存在するはず

# latestサーバーの設定を確認
ls -la /mc/spigot/latest/plugins/Kishax/
# → config.yml (common) と portals.yml (server-specific) が両方存在するはず
```

## 📋 サポートされるファイル種別

### server-specific/ 以下に配置可能なファイル

- `plugins/Kishax/portals.yml` - ポータル設定
- `plugins/Kishax/*.yml` - その他のKishaxプラグイン設定
- `plugins/LuckPerms/*.yml` - LuckPerms設定（サーバー別権限など）
- `config/*.yml` - Paper設定（サーバー別チューニングなど）

## ⚠️ 注意事項

1. **環境変数の置き換え**
   - テンプレートファイル内の `${VARIABLE}` は起動時に実際の値に置き換えられます
   - 例: `${MYSQL_HOST}`, `${MYSQL_PASSWORD}`

2. **ファイル上書き**
   - `server-specific/` のファイルは `common/` のファイルを**完全に上書き**します
   - 部分的なマージは行われません

3. **サーバー名の一致**
   - `server-specific/{サーバー名}/` のディレクトリ名は `servers.json` の `name` と完全一致する必要があります

4. **Git管理**
   - サーバー特有の設定もGitでバージョン管理されます
   - 機密情報（パスワードなど）は環境変数として `.env` で管理してください

## 🚀 実装例

### Example 1: homeサーバー専用のポータル設定

```yaml
# apps/mc/docker/templates/spigot/server-specific/home/plugins/Kishax/portals.yml
portals:
  spawn_to_nether:
    world: world
    location:
      x: 100
      y: 64
      z: 200
    destination:
      world: world_nether
      x: 12
      y: 64
      z: 25
    size:
      width: 3
      height: 3
```

### Example 2: latestサーバー専用のLuckPerms設定

```bash
# ディレクトリ作成
mkdir -p apps/mc/docker/templates/spigot/server-specific/latest/plugins/LuckPerms

# 設定ファイル作成
vim apps/mc/docker/templates/spigot/server-specific/latest/plugins/LuckPerms/config.yml
```

## 📚 関連ドキュメント

- [Docker Compose設定](../../compose.yml)
- [起動スクリプト](../scripts/start.sh)
- [Dockerfile](../../Dockerfile)
- [サーバー設定](../config/servers.json)
