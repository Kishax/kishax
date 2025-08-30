include .env

.PHONY: deploy deploy-plugin deploy-config mysql

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
