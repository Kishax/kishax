# Kishax Minecraft Server Development Environment

## QuickStart

```bash
cp .env.example .env

cd docker/data/
cp velocity-kishax-config.yml.example velocity-kishax-config.yml
cp spigot-kishax-config.yml.example spigot-kishax-config.yml

docker-compose up -d

# reflect build plugin into the container
make deploy-plugin
# reflect config into the container
make deploy-config
```
