include .env

.PHONY: help deploy deploy-plugin deploy-config mysql mc-proxy mc-home mc-latest mc-spigot mc-velocity mc-list logs-proxy logs-home logs-latest logs-velocity logs-spigot restart-proxy restart-home restart-latest restart-all servers-status download-jars update-servers check-diff env-load

.DEFAULT_GOAL := help

help: ## ヘルプを表示
	@echo "Kishax MC Plugins Makefile"
	@echo ""
	@echo "利用可能なコマンド:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

env-load: ## 環境変数を読み込み (.env と .env.auto)
	@echo "以下のコマンドを実行して環境変数を読み込んでください:"
	@echo ""
	@echo "  source .env && source .env.auto"
	@echo ""

deploy-plugin:
	./gradlew build -x test
	docker compose up -d kishax-server
	docker cp ./spigot/sv1_21_8/build/libs/Kishax-Spigot-1.21.8.jar kishax-minecraft:/mc/spigot/plugins/
	docker cp ./velocity/build/libs/Kishax-Velocity-3.4.0.jar kishax-minecraft:/mc/velocity/plugins/
	docker compose restart kishax-server

deploy-config:
	docker compose up -d kishax-server
	docker cp ./docker/data/spigot-kishax-config.yml kishax-minecraft:/mc/spigot/plugins/Kishax/config.yml
	docker cp ./docker/data/velocity-kishax-config.yml kishax-minecraft:/mc/velocity/plugins/kishax/config.yml
	docker compose restart kishax-server

deploy: deploy-plugin deploy-config

mysql: ## MySQLコンテナに接続
	@if [ "$(MAKECMDGOALS)" = "mysql" ]; then \
		echo "実行コマンド: docker exec -it kishax-mysql mysql -h 127.0.0.1 -u $(MYSQL_USER) -p'$(MYSQL_PASSWORD)'"; \
	fi
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-mysql; then \
		echo "⚠️  kishax-mysqlコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	docker exec -it kishax-mysql mysql -h 127.0.0.1 -u $(MYSQL_USER) -p'$(MYSQL_PASSWORD)'

mc-proxy: ## Proxyサーバーコンソールに接続
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "📡 Proxy (Velocity) コンソールに接続します..."
	@echo "終了するには Ctrl+A → D を押してください"
	docker exec -it kishax-minecraft screen -rx proxy

mc-home: ## Homeサーバーコンソールに接続
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🏠 Home (Spigot) コンソールに接続します..."
	@echo "終了するには Ctrl+A → D を押してください"
	docker exec -it kishax-minecraft screen -rx home

mc-latest: ## Latestサーバーコンソールに接続
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🚀 Latest (Spigot) コンソールに接続します..."
	@echo "終了するには Ctrl+A → D を押してください"
	docker exec -it kishax-minecraft screen -rx latest

mc-spigot: mc-home ## Spigotサーバーコンソールに接続 (エイリアス: mc-home)

mc-velocity: mc-proxy ## Velocityサーバーコンソールに接続 (エイリアス: mc-proxy)

mc-list: ## Minecraft画面セッション一覧を表示
	@if [ "$(MAKECMDGOALS)" = "mc-list" ]; then \
		echo "実行コマンド: docker exec -it kishax-minecraft screen -list"; \
	fi
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	docker exec -it kishax-minecraft screen -list

logs-proxy: ## Proxyログを表示
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	docker exec -it kishax-minecraft cat /mc/velocity/logs/latest.log

logs-home: ## Homeサーバーログを表示
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	docker exec -it kishax-minecraft cat /mc/spigot/home/logs/latest.log

logs-latest: ## Latestサーバーログを表示
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	docker exec -it kishax-minecraft cat /mc/spigot/latest/logs/latest.log

logs-velocity: logs-proxy ## Velocityログを表示 (エイリアス: logs-proxy)

logs-spigot: logs-home ## Spigotログを表示 (エイリアス: logs-home)

restart-proxy: ## Proxyサーバーを再起動
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🔄 Proxyサーバーを再起動します..."
	@docker exec -it kishax-minecraft screen -wipe || true
	docker exec -it kishax-minecraft bash -c "screen -S proxy -X quit 2>/dev/null || true; sleep 2; cd /mc/velocity && screen -dmS proxy java -Xmx\$$(grep 'PROXY_MEMORY=' /mc/runtime/proxies.env | cut -d'=' -f2) -jar velocity.jar"
	@sleep 3
	@echo "✅ Proxyサーバーを再起動しました"

restart-home: ## Homeサーバーを再起動
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🔄 Homeサーバーを再起動します..."
	@docker exec -it kishax-minecraft screen -wipe || true
	docker exec -it kishax-minecraft bash -c "screen -S home -X quit 2>/dev/null || true; sleep 2; . /mc/runtime/spigots.env && cd /mc/spigot/home && screen -dmS home java -Xmx\$$SPIGOT_0_MEMORY -jar /mc/spigot/\$$SPIGOT_0_FILENAME --nogui"
	@sleep 3
	@echo "✅ Homeサーバーを再起動しました"

restart-latest: ## Latestサーバーを再起動
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🔄 Latestサーバーを再起動します..."
	@docker exec -it kishax-minecraft screen -wipe || true
	docker exec -it kishax-minecraft bash -c "screen -S latest -X quit 2>/dev/null || true; sleep 2; . /mc/runtime/spigots.env && cd /mc/spigot/latest && screen -dmS latest java -Xmx\$$SPIGOT_1_MEMORY -jar /mc/spigot/\$$SPIGOT_1_FILENAME --nogui"
	@sleep 3
	@echo "✅ Latestサーバーを再起動しました"

restart-all: ## 全サーバーを再起動
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "🔄 全サーバーを再起動します..."
	@$(MAKE) restart-proxy
	@sleep 5
	@$(MAKE) restart-home
	@$(MAKE) restart-latest
	@echo "✅ 全サーバーを再起動しました"

servers-status: ## サーバー状態を表示
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "📊 サーバー状態:"
	@echo ""
	docker exec -it kishax-minecraft screen -list

download-jars: ## Paper/Velocity JARファイルをダウンロード
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "📥 JARファイルをダウンロード中..."
	@echo ""
	@docker exec -it kishax-minecraft bash -c ' \
		cd /mc/spigot && \
		echo "🔍 servers.jsonから設定を読み込み中..." && \
		SPIGOT_COUNT=$$(jq -r ".spigots | length" /mc/config/servers.json) && \
		for ((i=0; i<$$SPIGOT_COUNT; i++)); do \
			NAME=$$(jq -r ".spigots[$$i].name" /mc/config/servers.json); \
			URL=$$(jq -r ".spigots[$$i].url" /mc/config/servers.json); \
			FILENAME=$$(jq -r ".spigots[$$i].filename" /mc/config/servers.json); \
			MEMORY_RATIO=$$(jq -r ".spigots[$$i].memory_ratio" /mc/config/servers.json); \
			if (( $$(echo "$$MEMORY_RATIO == 0" | bc -l) )); then \
				echo "  ⏭️  $$NAME: スキップ (無効)"; \
				continue; \
			fi; \
			if [ -f "$$FILENAME" ]; then \
				echo "  ✅ $$NAME: $$FILENAME (既に存在)"; \
			else \
				echo "  📥 $$NAME: $$FILENAME をダウンロード中..."; \
				wget -q "$$URL" -O "$$FILENAME" && echo "     ✅ ダウンロード完了" || echo "     ❌ ダウンロード失敗"; \
			fi; \
		done && \
		echo "" && \
		echo "🔍 Velocity JARを確認中..." && \
		cd /mc/velocity && \
		VELOCITY_URL=$$(jq -r ".proxies[0].url" /mc/config/servers.json) && \
		VELOCITY_FILENAME=$$(jq -r ".proxies[0].filename" /mc/config/servers.json) && \
		if [ -f "$$VELOCITY_FILENAME" ]; then \
			echo "  ✅ Velocity: $$VELOCITY_FILENAME (既に存在)"; \
		else \
			echo "  📥 Velocity: $$VELOCITY_FILENAME をダウンロード中..."; \
			wget -q "$$VELOCITY_URL" -O "$$VELOCITY_FILENAME" && echo "     ✅ ダウンロード完了" || echo "     ❌ ダウンロード失敗"; \
		fi && \
		echo "" && \
		echo "✅ JARファイルのダウンロードが完了しました" \
	'

update-servers: ## servers.jsonの変更を適用（JARダウンロード＆再起動）
	@if ! docker ps --format "table {{.Names}}" | grep -q kishax-minecraft; then \
		echo "⚠️  kishax-minecraftコンテナが動作していません。docker compose up -d で起動してください。"; \
		exit 1; \
	fi
	@echo "📥 servers.jsonの変更を適用します..."
	@echo ""
	@echo "⚠️  この操作は以下を実行します:"
	@echo "  1. 新しいPaper/Velocity JARファイルをダウンロード"
	@echo "  2. プラグインを再配置"
	@echo "  3. 設定を更新"
	@echo "  4. 全サーバーを再起動"
	@echo ""
	@read -p "続行しますか？ (y/N): " answer; \
	if [ "$$answer" != "y" ] && [ "$$answer" != "Y" ]; then \
		echo "キャンセルしました"; \
		exit 0; \
	fi; \
	echo ""; \
	echo "📥 JARファイルをダウンロード中..."; \
	$(MAKE) download-jars; \
	echo ""; \
	echo "🔧 セットアップスクリプトを実行中..."; \
	docker exec -it kishax-minecraft /mc/scripts/setup-directories.sh; \
	docker exec -it kishax-minecraft /mc/scripts/deploy-plugins.sh; \
	docker exec -it kishax-minecraft /mc/scripts/calculate-memory.sh; \
	docker exec -it kishax-minecraft /mc/scripts/generate-velocity-config.sh; \
	echo ""; \
	echo "🔄 全サーバーを再起動中..."; \
	$(MAKE) restart-all; \
	echo ""; \
	echo "✅ 更新が完了しました！"

check-diff: ## servers.jsonの変更差分を確認
	@echo "📋 servers.jsonの変更内容:"
	@echo ""
	@if command -v git >/dev/null 2>&1; then \
		git diff docker/config/servers.json || echo "変更なし"; \
	else \
		echo "gitコマンドが見つかりません"; \
	fi
