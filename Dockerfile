# Multi-stage build for Kishax Minecraft Server Environment
FROM openjdk:21-jdk-slim as builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    gradle \
    git \
    wget \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Set working directory for build
WORKDIR /app

# Copy project files
COPY . .

# Build the Kishax plugins
RUN ./gradlew build -x test

# Production stage
FROM openjdk:21-jdk-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    mariadb-client \
    wget \
    curl \
    openssl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /mc/spigot/plugins \
    && mkdir -p /mc/velocity/plugins \
    && mkdir -p /mc/server/images

# Set working directory
WORKDIR /mc

# Copy URLs configuration
COPY docker/data/urls.json /mc/config/urls.json

# Download servers and plugins based on URLs config
RUN PAPER_URL=$(jq -r '.minecraft_servers.paper.url' /mc/config/urls.json) && \
    PAPER_FILENAME=$(jq -r '.minecraft_servers.paper.filename' /mc/config/urls.json) && \
    VELOCITY_URL=$(jq -r '.minecraft_servers.velocity.url' /mc/config/urls.json) && \
    VELOCITY_FILENAME=$(jq -r '.minecraft_servers.velocity.filename' /mc/config/urls.json) && \
    LUCKPERMS_BUKKIT_URL=$(jq -r '.plugins.luckperms.bukkit.url' /mc/config/urls.json) && \
    LUCKPERMS_BUKKIT_FILENAME=$(jq -r '.plugins.luckperms.bukkit.filename' /mc/config/urls.json) && \
    LUCKPERMS_VELOCITY_URL=$(jq -r '.plugins.luckperms.velocity.url' /mc/config/urls.json) && \
    LUCKPERMS_VELOCITY_FILENAME=$(jq -r '.plugins.luckperms.velocity.filename' /mc/config/urls.json) && \
    GEYSER_VELOCITY_URL=$(jq -r '.plugins.geyser.velocity.url' /mc/config/urls.json) && \
    GEYSER_VELOCITY_FILENAME=$(jq -r '.plugins.geyser.velocity.filename' /mc/config/urls.json) && \
    FLOODGATE_VELOCITY_URL=$(jq -r '.plugins.floodgate.velocity.url' /mc/config/urls.json) && \
    FLOODGATE_VELOCITY_FILENAME=$(jq -r '.plugins.floodgate.velocity.filename' /mc/config/urls.json) && \
    echo "Downloading Paper server..." && \
    wget -O "spigot/$PAPER_FILENAME" "$PAPER_URL" && \
    echo "Downloading Velocity server..." && \
    wget -O "velocity/$VELOCITY_FILENAME" "$VELOCITY_URL" && \
    echo "Downloading LuckPerms Bukkit..." && \
    wget -O "spigot/plugins/$LUCKPERMS_BUKKIT_FILENAME" "$LUCKPERMS_BUKKIT_URL" && \
    echo "Downloading LuckPerms Velocity..." && \
    wget -O "velocity/plugins/$LUCKPERMS_VELOCITY_FILENAME" "$LUCKPERMS_VELOCITY_URL" && \
    echo "Downloading Geyser Velocity..." && \
    wget -O "velocity/plugins/$GEYSER_VELOCITY_FILENAME" "$GEYSER_VELOCITY_URL" && \
    echo "Downloading Floodgate Velocity..." && \
    wget -O "velocity/plugins/$FLOODGATE_VELOCITY_FILENAME" "$FLOODGATE_VELOCITY_URL"

# Copy built Kishax plugins from builder stage
COPY --from=builder /app/spigot/build/libs/*.jar spigot/plugins/
COPY --from=builder /app/velocity/build/libs/*.jar velocity/plugins/

# Copy config files
COPY docker/mc/spigot/config/ spigot/config/
COPY docker/mc/spigot/server.properties spigot/server.properties
COPY docker/mc/velocity/ velocity/config/

# Copy LuckPerms plugin configs
COPY docker/mc/spigot/plugins/LuckPerms/config.yml spigot/plugins/LuckPerms/config.yml
COPY docker/mc/velocity/plugins/luckperms/config.yml velocity/plugins/luckperms/config.yml

# Copy Kishax plugin configs
COPY spigot/src/main/resources/config.yml spigot/plugins/Kishax/config.yml
COPY velocity/src/main/resources/config.yml velocity/plugins/Kishax/config.yml

# MySQL initialization is handled by docker-compose volume mount

# Copy startup script
COPY docker/scripts/start.sh /mc/start.sh
RUN chmod +x /mc/start.sh

# Accept Minecraft EULA
RUN echo "eula=true" > spigot/eula.txt

# Expose ports
EXPOSE 25565 25577

# Start script
CMD ["./start.sh"]