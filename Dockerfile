# Multi-stage build for Kishax Minecraft Server Environment
FROM eclipse-temurin:21-jdk AS builder

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
FROM eclipse-temurin:21-jdk

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    mariadb-client \
    wget \
    curl \
    unzip \
    openssl \
    jq \
    screen \
    bc \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" \
    && unzip awscliv2.zip \
    && ./aws/install \
    && rm -rf awscliv2.zip aws

# Create directories
RUN mkdir -p /mc/spigot/plugins \
    && mkdir -p /mc/velocity/plugins \
    && mkdir -p /mc/server/images \
    && mkdir -p /mc/scripts \
    && mkdir -p /mc/runtime

# Set working directory
WORKDIR /mc

# Copy servers configuration
COPY docker/config/servers.json /mc/config/servers.json

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
RUN LUCKPERMS_VELOCITY_URL=$(jq -r '.plugins.luckperms.velocity.url' /mc/config/servers.json) && \
    LUCKPERMS_VELOCITY_FILENAME=$(jq -r '.plugins.luckperms.velocity.filename' /mc/config/servers.json) && \
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

# Copy built Kishax plugins from builder stage to temporary build directory
RUN mkdir -p /mc/build/spigot /mc/build/velocity
COPY --from=builder /app/spigot/sv1_21_11/build/libs/ /mc/build/spigot/sv1_21_11/
COPY --from=builder /app/spigot/sv1_21_8/build/libs/ /mc/build/spigot/sv1_21_8/
COPY --from=builder /app/velocity/build/libs/ /mc/build/velocity/
RUN find /mc/build/spigot -name "*.jar" -exec cp {} /mc/build/spigot/ \; && \
    ls -la /mc/build/spigot/ && ls -la /mc/build/velocity/

# Copy template directories (will be copied to actual locations at runtime)
COPY docker/templates/spigot /mc/templates/spigot
COPY docker/templates/velocity /mc/templates/velocity

# Copy LuckPerms plugin configs are already in templates
# Copy Kishax plugin configs are already in templates

# MySQL initialization files
COPY docker/database/schema/TABLES.sql /mc/mysql/init/
COPY docker/database/seeds/ /mc/mysql/seeds/


# MySQL initialization is handled by docker-compose volume mount

# Copy startup scripts
COPY docker/scripts/start.sh /mc/start.sh
COPY docker/scripts/setup-directories.sh /mc/scripts/setup-directories.sh
COPY docker/scripts/calculate-memory.sh /mc/scripts/calculate-memory.sh
COPY docker/scripts/generate-velocity-config.sh /mc/scripts/generate-velocity-config.sh
COPY docker/scripts/deploy-plugins.sh /mc/scripts/deploy-plugins.sh
COPY docker/scripts/register-servers-to-db.sh /mc/scripts/register-servers-to-db.sh
COPY docker/scripts/import-world-from-s3.sh /mc/scripts/import-world-from-s3.sh
RUN chmod +x /mc/start.sh /mc/scripts/*.sh

# Accept Minecraft EULA
RUN echo "eula=true" > spigot/eula.txt

# Expose ports
EXPOSE 25565 25577

# Start script
CMD ["./start.sh"]
