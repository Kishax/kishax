# Kishax Minecraft Server Development Environment

## QuickStart

```bash
cp .env.example .env
cp docker/data/velocity-kishax-config.yml.example docker/data/velocity-kishax-config.yml
cp docker/data/spigot-kishax-config.yml.example docker/data/spigot-kishax-config.yml
```

```bash
docker-compose up -d
```
```bash
# reflect build plugin into the container
make deploy-plugin
# reflect config into the container
make deploy-config
```
