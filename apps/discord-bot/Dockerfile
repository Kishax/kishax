# Discord Bot Dockerfile
FROM openjdk:17-jdk-slim

# 作業ディレクトリを設定
WORKDIR /app

# 必要なツールをインストール
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Gradleビルド用にソースをコピー
COPY build.gradle .
COPY src ./src

# Gradle Wrapperをコピー
COPY gradle ./gradle
COPY gradlew .
COPY settings.gradle* ./

# 実行可能権限を付与
RUN chmod +x ./gradlew

# 依存関係をダウンロードしてビルド
RUN ./gradlew build --no-daemon

# ログディレクトリを作成
RUN mkdir -p /app/logs

# 非rootユーザーを作成
RUN useradd -r -u 1000 discord-bot && \
    chown -R discord-bot:discord-bot /app

# ユーザーを切り替え
USER discord-bot

# ヘルスチェック
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# アプリケーションを実行
CMD ["java", "-jar", "/app/build/libs/discord-bot-1.0.0.jar"]