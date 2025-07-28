# CMD

## pm2

```bash
# プロセス一覧
pm2 list

# 詳細情報
pm2 show gather-bot

# メモリ・CPU使用率
pm2 monit

# ログローテーション（pm2-logrotateインストール後）
pm2 install pm2-logrotate

# 1. コード更新
git pull origin main

# 2. 依存関係更新
npm install

# 3. 無停止再起動
pm2 reload gather-bot

# または通常再起動
pm2 restart gather-bot

# PM2完全リセット
pm2 kill
pm2 start ecosystem.config.js
```
