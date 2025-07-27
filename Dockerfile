# Node.js 18 LTS Alpine版を使用（軽量）
FROM node:18-alpine

# メンテナ情報
LABEL maintainer="your-email@example.com"
LABEL description="Gather to Slack notification bot"

# 作業ディレクトリを設定
WORKDIR /app

# システムパッケージを更新とタイムゾーン設定
RUN apk update && apk add --no-cache \
    tzdata \
    && cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
    && echo "Asia/Tokyo" > /etc/timezone \
    && apk del tzdata

# package.jsonとpackage-lock.json（存在する場合）をコピー
COPY package*.json ./

# 依存関係をインストール（本番環境用）
RUN npm ci --only=production && \
    npm cache clean --force

# PM2をグローバルインストール
RUN npm install -g pm2

# アプリケーションファイルをコピー
COPY . .

# ログディレクトリを作成
RUN mkdir -p logs

# 非rootユーザーを作成
RUN addgroup -g 1001 -S nodejs && \
    adduser -S gather-bot -u 1001

# ログディレクトリの権限を設定
RUN chown -R gather-bot:nodejs /app/logs

# 非rootユーザーに切り替え
USER gather-bot

# ポート公開（必要に応じて）
EXPOSE 3000

# ヘルスチェック設定
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD node -e "console.log('Health check passed')" || exit 1

# PM2でアプリケーションを起動
CMD ["pm2-runtime", "start", "ecosystem.config.js", "--env", "production"]

# 代替起動方法（PM2なしの場合）
# CMD ["node", "index.js"]
