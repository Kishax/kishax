.PHONY: deploy deploy-plugin deploy-config

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
