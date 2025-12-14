# Multi-stage build for Kishax Minecraft Server Environment
FROM openjdk:21-jdk-slim AS builder

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
RUN if [ ! -f velocity/build/libs/Kishax-Velocity-*.jar ] || [ ! -f spigot/sv1_21_8/build/libs/Kishax-Spigot-*.jar ]; then \
        echo "JARs not found, building from source..."; \
        ./gradlew build -x test; \
    else \
        echo "Kishax plugins already built, skipping build step"; \
    fi

# Production stage
FROM openjdk:21-jdk-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    mariadb-client \
    wget \
    curl \
    openssl \
    jq \
    screen \
    bc \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /mc/spigot/plugins \
    && mkdir -p /mc/velocity/plugins \
    && mkdir -p /mc/server/images \
    && mkdir -p /mc/scripts \
    && mkdir -p /mc/runtime

# Set working directory
WORKDIR /mc

# Copy servers configuration
COPY docker/data/servers.json /mc/config/servers.json

# Download Paper server
RUN PAPER_URL=$(jq -r '.spigots[0].url' /mc/config/servers.json) && \
    PAPER_FILENAME=$(jq -r '.spigots[0].filename' /mc/config/servers.json) && \
    echo "Downloading Paper server: $PAPER_FILENAME from $PAPER_URL" && \
    wget -O "spigot/$PAPER_FILENAME" "$PAPER_URL" && \
    echo "Paper server download completed"

# Download Velocity server
RUN VELOCITY_URL=$(jq -r '.proxies[0].url' /mc/config/servers.json) && \
    VELOCITY_FILENAME=$(jq -r '.proxies[0].filename' /mc/config/servers.json) && \
    echo "Downloading Velocity server: $VELOCITY_FILENAME from $VELOCITY_URL" && \
    wget -O "velocity/$VELOCITY_FILENAME" "$VELOCITY_URL" && \
    echo "Velocity server download completed"

# Download LuckPerms Bukkit
RUN LUCKPERMS_BUKKIT_URL=$(jq -r '.plugins.luckperms.bukkit.url' /mc/config/servers.json) && \
    LUCKPERMS_BUKKIT_FILENAME=$(jq -r '.plugins.luckperms.bukkit.filename' /mc/config/servers.json) && \
    echo "Downloading LuckPerms Bukkit: $LUCKPERMS_BUKKIT_FILENAME from $LUCKPERMS_BUKKIT_URL" && \
    wget -O "spigot/plugins/$LUCKPERMS_BUKKIT_FILENAME" "$LUCKPERMS_BUKKIT_URL" && \
    echo "LuckPerms Bukkit download completed"

# Download LuckPerms Velocity
RUN LUCKPERMS_VELOCITY_URL=$(jq -r '.plugins["luckperms-velocity"].url' /mc/config/servers.json) && \
    LUCKPERMS_VELOCITY_FILENAME=$(jq -r '.plugins["luckperms-velocity"].filename' /mc/config/servers.json) && \
    echo "Downloading LuckPerms Velocity: $LUCKPERMS_VELOCITY_FILENAME from $LUCKPERMS_VELOCITY_URL" && \
    wget -O "velocity/plugins/$LUCKPERMS_VELOCITY_FILENAME" "$LUCKPERMS_VELOCITY_URL" && \
    echo "LuckPerms Velocity download completed"

# Download Geyser Velocity
RUN GEYSER_VELOCITY_URL=$(jq -r '.plugins["geyser-velocity"].url' /mc/config/servers.json) && \
    GEYSER_VELOCITY_FILENAME=$(jq -r '.plugins["geyser-velocity"].filename' /mc/config/servers.json) && \
    echo "Downloading Geyser Velocity: $GEYSER_VELOCITY_FILENAME from $GEYSER_VELOCITY_URL" && \
    wget -O "velocity/plugins/$GEYSER_VELOCITY_FILENAME" "$GEYSER_VELOCITY_URL" && \
    echo "Geyser Velocity download completed"

# Download Floodgate Velocity
RUN FLOODGATE_VELOCITY_URL=$(jq -r '.plugins["floodgate-velocity"].url' /mc/config/servers.json) && \
    FLOODGATE_VELOCITY_FILENAME=$(jq -r '.plugins["floodgate-velocity"].filename' /mc/config/servers.json) && \
    echo "Downloading Floodgate Velocity: $FLOODGATE_VELOCITY_FILENAME from $FLOODGATE_VELOCITY_URL" && \
    wget -O "velocity/plugins/$FLOODGATE_VELOCITY_FILENAME" "$FLOODGATE_VELOCITY_URL" && \
    echo "Floodgate Velocity download completed"

# Copy built Kishax plugins from builder stage
COPY --from=builder /app/spigot/sv1_21_8/build/libs/Kishax-Spigot-1.21.8.jar spigot/plugins/
# Remove unwanted velocity jar files and copy only the correct one
RUN rm -f /tmp/velocity-build/* || true
COPY --from=builder /app/velocity/build/libs/ /tmp/velocity-build/
RUN cp /tmp/velocity-build/Kishax-Velocity-3.4.0.jar velocity/plugins/ && rm -rf /tmp/velocity-build

# Copy config files
COPY docker/mc/spigot/config/ spigot/config/
COPY docker/mc/spigot/server.properties spigot/server.properties
COPY docker/mc/velocity/ velocity/config/
COPY docker/mc/velocity/velocity.toml velocity/velocity.toml

# Copy LuckPerms plugin configs
COPY docker/mc/spigot/plugins/LuckPerms/config.yml spigot/plugins/LuckPerms/config.yml
COPY docker/mc/velocity/plugins/luckperms/config.yml velocity/plugins/luckperms/config.yml

# Copy Kishax plugin configs (use custom configs if available, otherwise use defaults)
COPY docker/data/spigot-kishax-config.yml* spigot/plugins/Kishax/
COPY docker/data/velocity-kishax-config.yml* velocity/plugins/kishax/
RUN if [ -f "spigot/plugins/Kishax/spigot-kishax-config.yml" ]; then \
        mv spigot/plugins/Kishax/spigot-kishax-config.yml spigot/plugins/Kishax/config.yml; \
    else \
        cp spigot/src/main/resources/config.yml spigot/plugins/Kishax/config.yml; \
    fi
RUN if [ -f "velocity/plugins/kishax/velocity-kishax-config.yml" ]; then \
        mv velocity/plugins/kishax/velocity-kishax-config.yml velocity/plugins/kishax/config.yml; \
    else \
        cp velocity/src/main/resources/config.yml velocity/plugins/kishax/config.yml; \
    fi


# MySQL initialization is handled by docker-compose volume mount

# Copy startup scripts
COPY docker/scripts/start.sh /mc/start.sh
COPY docker/scripts/calculate-memory.sh /mc/scripts/calculate-memory.sh
RUN chmod +x /mc/start.sh /mc/scripts/calculate-memory.sh

# Accept Minecraft EULA
RUN echo "eula=true" > spigot/eula.txt

# Expose ports
EXPOSE 25565 25577

# Start script
CMD ["./start.sh"]
