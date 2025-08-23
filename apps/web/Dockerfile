# Multi-stage Dockerfile for development and production
FROM node:22-alpine AS base

# 作業ディレクトリを設定
WORKDIR /app

# 開発に必要なパッケージをインストール
RUN apk add --no-cache git

# package.jsonとpackage-lock.jsonをコピー
COPY package*.json ./
COPY prisma ./prisma/

# 依存関係をインストール（devDependenciesも含む、ビルド時に必要）
RUN npm ci

# Prismaクライアントを生成
RUN npx prisma generate

# Development stage
FROM base AS development
ENV NODE_ENV=development
EXPOSE 3000
CMD ["npm", "run", "dev"]

# Production stage
FROM base AS production
ENV NODE_ENV=production

# アプリケーションのソースコードをコピー
COPY . .

# Next.jsアプリケーションをビルド
RUN npm run build

# 非rootユーザーを作成
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# ファイルの所有権を変更
RUN chown -R nextjs:nodejs /app
USER nextjs

# ポート3000を公開
EXPOSE 3000

# アプリケーションを起動
CMD ["npm", "start"]

# Default to production
FROM production