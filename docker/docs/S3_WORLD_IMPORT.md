# S3 World Data Import System

## 概要

S3に保存されたワールドデータを、サーバー初回起動時に自動的にインポートする機能。

## S3ディレクトリ構造

```
s3://kishax-production-world-backups/deployment/
├── 202506/                          # YYYYMM形式のディレクトリ
│   └── 1/                           # バージョン番号
│       └── latest/                  # サーバー名
│           ├── __IMPORT_ENABLED__   # インポート許可フラグ（空ファイル）
│           ├── world/               # オーバーワールド
│           │   ├── level.dat
│           │   ├── region/
│           │   └── ...
│           ├── world_nether/        # ネザー
│           │   └── ...
│           └── world_the_end/       # エンド
│               └── ...
└── 202512/                          # 新しい月
    └── 1/
        └── latest/
            └── ...
```

## インポート条件（3段階チェック）

### 1. servers.jsonでの設定
```json
{
  "name": "latest",
  "s3import": true,  // ← trueの場合のみインポート対象
  ...
}
```

### 2. S3での許可フラグ
- `__IMPORT_ENABLED__`ファイルが存在すること
- 最新の`YYYYMM`ディレクトリ内を検索
- 例: `s3://kishax-production-world-backups/deployment/202512/1/latest/__IMPORT_ENABLED__`

### 3. ローカルボリュームでの初回確認
- `/mc/volumes/{server_name}/.import_completed`が**存在しない**こと
- このファイルが存在する場合、すでにインポート済みと判断

## ワークフロー

```mermaid
graph TD
    A[Docker起動] --> B{servers.json<br/>s3import=true?}
    B -->|No| Z[通常起動]
    B -->|Yes| C{ボリュームに<br/>.import_completed<br/>存在?}
    C -->|Yes| Z
    C -->|No| D[S3から最新YYYYMM<br/>ディレクトリ検索]
    D --> E{__IMPORT_ENABLED__<br/>ファイル存在?}
    E -->|No| Z
    E -->|Yes| F[S3からworld/world_nether/<br/>world_the_endをダウンロード]
    F --> G[/mc/spigot/{server}/へ展開]
    G --> H[テンプレートファイル適用<br/>paper-global.yml等]
    H --> I[.import_completedフラグ作成]
    I --> J[サーバー起動]
```

## 環境変数

```bash
# .env に追加
S3_BUCKET=kishax-production-world-backups
S3_WORLDS_PREFIX=deployment/
AWS_REGION=ap-northeast-1
```

## 実装ファイル

### 1. `docker/scripts/import-world-from-s3.sh`
- S3からワールドデータをダウンロード
- 引数: サーバー名（例: `latest`）
- 戻り値: 0=成功, 1=スキップ, 2=エラー

### 2. `docker/scripts/start.sh` (修正)
- スピゴット起動前に`import-world-from-s3.sh`を呼び出し

## AWS CLI コマンド例

```bash
# 最新のYYYYMMディレクトリを取得
aws s3 ls s3://kishax-production-world-backups/deployment/ \
  --recursive | grep '__IMPORT_ENABLED__' | grep '/latest/' | sort -r | head -1

# ワールドデータをダウンロード
aws s3 sync s3://kishax-production-world-backups/deployment/202512/1/latest/world/ /mc/spigot/latest/world/
aws s3 sync s3://kishax-production-world-backups/deployment/202512/1/latest/world_nether/ /mc/spigot/latest/world_nether/
aws s3 sync s3://kishax-production-world-backups/deployment/202512/1/latest/world_the_end/ /mc/spigot/latest/world_the_end/
```

## セキュリティ考慮事項

1. **2段階認証**:
   - `s3import: true` (開発者が設定)
   - `__IMPORT_ENABLED__` (管理者がS3に配置)

2. **べき等性**:
   - `.import_completed`フラグで2回目以降のインポートを防止

3. **データ整合性**:
   - `aws s3 sync`で差分同期
   - ダウンロード失敗時はエラーログ出力

## トラブルシューティング

### インポートがスキップされる
```bash
# ボリューム内のフラグを確認
docker exec kishax-minecraft ls -la /mc/volumes/latest/.import_completed

# S3のフラグを確認
aws s3 ls s3://kishax-production-world-backups/deployment/ --recursive | grep '__IMPORT_ENABLED__'
```

### 強制再インポート
```bash
# フラグを削除
docker exec kishax-minecraft rm /mc/volumes/latest/.import_completed

# コンテナ再起動
docker compose restart
```

### S3アクセスエラー
```bash
# IAM権限確認
aws s3 ls s3://kishax-production-world-backups/deployment/ --profile AdministratorAccess-126112056177

# EC2インスタンスのIAMロール確認（本番環境）
aws iam get-role --role-name kishax-production-ec2-role
```

## 今後の拡張

- [ ] バージョン番号を環境変数で指定可能に
- [ ] インポート進捗をログに出力
- [ ] インポート完了後にSlack/Discord通知
- [ ] ワールドデータの自動バックアップ（逆方向のS3アップロード）






