#!/bin/bash
set -e

# Configuration file path
CONFIG_FILE="/mc/config/servers.json"

echo "Starting Kishax Minecraft Server Environment..."

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
while ! mysql -h${MYSQL_HOST:-mysql} -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-password} -e "SELECT 1" >/dev/null 2>&1; do
  echo "MySQL is not ready, waiting..."
  sleep 5
done

# Initialize database
echo "Initializing database..."
mysql -h${MYSQL_HOST:-mysql} -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-password} -e "CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE:-mc};"

# Import SQL files
for sql_file in /mc/mysql/init/*.sql; do
  if [ -f "$sql_file" ]; then
    echo "Importing $(basename $sql_file)..."
    mysql -h${MYSQL_HOST:-mysql} -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-password} ${MYSQL_DATABASE:-mc} <"$sql_file"
  fi
done

# Setup directories from templates
echo "Setting up server directories from templates..."
/mc/scripts/setup-directories.sh

# Register servers to database from servers.json
echo "Registering servers to database..."
/mc/scripts/register-servers-to-db.sh

# Deploy plugins based on servers.json
echo "Deploying plugins..."
/mc/scripts/deploy-plugins.sh

# Calculate memory allocation from servers.json
echo "Calculating memory allocation..."
/mc/scripts/calculate-memory.sh

# Generate Velocity configuration files
echo "Generating Velocity configuration..."
/mc/scripts/generate-velocity-config.sh

# Load calculated configurations
source /mc/runtime/proxies.env
source /mc/runtime/spigots.env
source /mc/runtime/velocity-config.env

# Export HOME_SERVER variables for use in config files
export HOME_SERVER_NAME
export HOME_SERVER_IP="127.0.0.1"

echo "Active Proxies: $ACTIVE_PROXY_COUNT"
echo "Active Spigots: $ACTIVE_SPIGOT_COUNT"
echo "Home Server: $HOME_SERVER_NAME at $HOME_SERVER_IP"

# velocity.tomlの[servers]セクションを置き換え
echo "Updating velocity.toml with dynamic server configuration..."
VELOCITY_TOML="/mc/velocity/velocity.toml"

# velocity.tomlが存在しない場合、テンプレートからコピー
if [ ! -f "$VELOCITY_TOML" ]; then
    echo "velocity.toml not found, copying from template..."
    cp /mc/templates/velocity/velocity.toml "$VELOCITY_TOML"
fi

# より安全な方法: awk を使用して[servers]から[forced-hosts]までを置換
awk -v servers="$VELOCITY_SERVERS_SECTION" -v try_list="$VELOCITY_TRY_LIST" -v home_server="$HOME_SERVER_NAME" '
/^\[servers\]/ {
    print $0
    print "# ================================================================"
    print "# This section is dynamically generated from servers.json at startup"
    print "# Do not edit manually - it will be overwritten"
    print "# ================================================================"
    print "# Configure your servers here. Each key represents the server'"'"'s name, and the value"
    print "# represents the IP address of the server to connect to."
    printf "%s", servers
    print ""
    print "# In what order we should try servers when a player logs in or is kicked from a server."
    print "try = ["
    print "    " try_list
    print "]"
    print ""
    skip=1
    next
}
/^\[forced-hosts\]/ {
    print $0
    print "# ================================================================"
    print "# This section is dynamically generated from servers.json at startup"
    print "# Do not edit manually - it will be overwritten"
    print "# ================================================================"
    print "#Configure your forced hosts here."
    if (home_server != "") {
        print "\"mc.kishax.net\" = [\"" home_server "\"]"
    }
    skip=1
    next
}
/^\[advanced\]/ {
    skip=0
}
!skip
' "$VELOCITY_TOML" > "$VELOCITY_TOML.tmp" && mv "$VELOCITY_TOML.tmp" "$VELOCITY_TOML"

echo "velocity.toml updated successfully"

# velocity-kishax-config.ymlのServersセクションを置き換え
echo "Updating velocity-kishax-config.yml with dynamic server configuration..."
VELOCITY_KISHAX_CONFIG="/mc/velocity/plugins/kishax/config.yml"

# まだコピーされていない場合は、テンプレートからコピー
if [ ! -f "$VELOCITY_KISHAX_CONFIG" ]; then
    mkdir -p /mc/velocity/plugins/kishax
    cp /mc/templates/velocity/plugins/kishax/config.yml "$VELOCITY_KISHAX_CONFIG" || true
fi

# Serversセクションを動的に置き換え
if [ -f "$VELOCITY_KISHAX_CONFIG" ]; then
    # Servers:以降を削除
    sed -i '/^Servers:/,$d' "$VELOCITY_KISHAX_CONFIG"
    # 動的に生成したServersセクションを追加
    cat /mc/runtime/kishax-servers.yml >> "$VELOCITY_KISHAX_CONFIG"
    echo "velocity-kishax-config.yml updated successfully"
fi

# Check if forwarding.secret already exists
if [ -f "/mc/velocity/forwarding.secret" ]; then
  # Use existing secret
  FORWARDING_SECRET=$(cat /mc/velocity/forwarding.secret)
  echo "Using existing forwarding secret: $FORWARDING_SECRET"
else
  # Generate new secret for first time
  FORWARDING_SECRET=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9' | head -c 12)
  echo "Generated new forwarding secret: $FORWARDING_SECRET"

  # Create forwarding.secret file for Velocity
  echo "$FORWARDING_SECRET" >/mc/velocity/forwarding.secret
  echo "Created forwarding.secret file for Velocity"
fi

# Replace placeholders in config files
echo "Configuring server files..."
find /mc -type f \( -name "*.yml" -o -name "*.toml" \) | while read file; do
  echo "Processing: $file"

  # Apply all replacements
  sed -i.bak "s|\${THIS_IS_SECRET}|${FORWARDING_SECRET}|g" "$file"
  sed -i.bak "s|\${MYSQL_HOST}|${MYSQL_HOST:-mysql}|g" "$file"
  sed -i.bak "s|\${MYSQL_DATABASE}|${MYSQL_DATABASE:-mc}|g" "$file"
  sed -i.bak "s|\${MYSQL_PORT}|${MYSQL_PORT:-3306}|g" "$file"
  sed -i.bak "s|\${MYSQL_USER}|${MYSQL_USER:-root}|g" "$file"
  sed -i.bak "s|\${MYSQL_PASSWORD}|${MYSQL_PASSWORD:-password}|g" "$file"
  sed -i.bak "s|\${CONFIRM_URL:-https://your-confirm-url.com}|${CONFIRM_URL:-https://your-confirm-url.com}|g" "$file"
  sed -i.bak "s|\${HOME_SERVER_NAME:-spigot}|${HOME_SERVER_NAME:-spigot}|g" "$file"
  sed -i.bak "s|\${HOME_SERVER_IP:-127.0.0.1}|${HOME_SERVER_IP:-127.0.0.1}|g" "$file"
  # AWS Configuration replacements
  sed -i.bak "s|\${AWS_REGION}|${AWS_REGION:-ap-northeast-1}|g" "$file"
  # MC-Web SQS Configuration replacements
  sed -i.bak "s|\${MC_WEB_SQS_ACCESS_KEY_ID}|${MC_WEB_SQS_ACCESS_KEY_ID}|g" "$file"
  sed -i.bak "s|\${MC_WEB_SQS_SECRET_ACCESS_KEY}|${MC_WEB_SQS_SECRET_ACCESS_KEY}|g" "$file"
  # SQS Queue URLs (New naming)
  sed -i.bak "s|\${TO_MC_QUEUE_URL}|${TO_MC_QUEUE_URL}|g" "$file"
  sed -i.bak "s|\${TO_WEB_QUEUE_URL}|${TO_WEB_QUEUE_URL}|g" "$file"
  sed -i.bak "s|\${TO_DISCORD_QUEUE_URL}|${TO_DISCORD_QUEUE_URL}|g" "$file"
  # AUTH_API_URL, AUTH_API_KEY replacements
  sed -i.bak "s|\${AUTH_API_URL}|${AUTH_API_URL:-http://host.docker.internal:8080}|g" "$file"
  sed -i.bak "s|\${AUTH_API_KEY}|${AUTH_API_KEY:-local-dev-api-key}|g" "$file"
  # REDIS_URL replacement
  sed -i.bak "s|\${REDIS_URL}|${REDIS_URL:-redis://redis:6379}|g" "$file"
  rm -f "$file.bak"

  # Show key replacements for critical files
  if [[ "$file" == *"paper-global.yml"* ]]; then
    SECRET_VALUE=$(grep "secret:" "$file" | head -1 | awk '{print $2}')
    echo "  paper-global.yml secret: $SECRET_VALUE"
  fi
  if [[ "$file" == *"config.yml"* ]] && [[ "$file" == *"kishax"* ]]; then
    AWS_KEY=$(grep "AccessKey:" "$file" | head -1 | awk '{print $2}' | cut -c1-10)
    echo "  kishax config AWS key: ${AWS_KEY}..."
  fi
done

# Copy processed template files to actual server directories
echo "Copying processed templates to server directories..."

# Copy Spigot config files
if [ -f "/mc/templates/spigot/config/paper-global.yml" ]; then
    mkdir -p /mc/spigot/config
    cp /mc/templates/spigot/config/paper-global.yml /mc/spigot/config/paper-global.yml
    echo "  Copied paper-global.yml to /mc/spigot/config/"
fi

# Copy Spigot plugin configs
if [ -d "/mc/templates/spigot/plugins" ]; then
    mkdir -p /mc/spigot/plugins
    cp -r /mc/templates/spigot/plugins/Kishax /mc/spigot/plugins/ 2>/dev/null || true
    cp -r /mc/templates/spigot/plugins/LuckPerms /mc/spigot/plugins/ 2>/dev/null || true
    echo "  Copied plugin configs to /mc/spigot/plugins/"
fi

# Copy Velocity plugin configs (already done earlier, but ensure consistency)
if [ -d "/mc/templates/velocity/plugins" ]; then
    mkdir -p /mc/velocity/plugins
    cp -r /mc/templates/velocity/plugins/kishax /mc/velocity/plugins/ 2>/dev/null || true
    cp -r /mc/templates/velocity/plugins/luckperms /mc/velocity/plugins/ 2>/dev/null || true
    echo "  Copied Velocity plugin configs to /mc/velocity/plugins/"
fi

# Final verification
VELOCITY_SECRET=$(cat /mc/velocity/forwarding.secret)
if [ -f "/mc/spigot/config/paper-global.yml" ]; then
    PAPER_SECRET=$(grep "secret:" /mc/spigot/config/paper-global.yml | head -1 | awk '{print $2}')
    echo "Final check - Velocity: $VELOCITY_SECRET, Paper: $PAPER_SECRET"
    if [ "$VELOCITY_SECRET" = "$PAPER_SECRET" ]; then
      echo "✅ Forwarding secrets match!"
    else
      echo "❌ Forwarding secrets do NOT match!"
    fi
else
    echo "⚠️  paper-global.yml not found yet, will be created on first run"
fi

# Ensure plugins directories exist
mkdir -p /mc/spigot/plugins/Kishax
mkdir -p /mc/velocity/plugins/kishax
mkdir -p /mc/spigot/plugins/LuckPerms
mkdir -p /mc/velocity/plugins/luckperms

# Update Kishax plugin configs with MySQL credentials
echo "Updating Kishax plugin configurations with MySQL credentials..."
find /mc/spigot/plugins/Kishax /mc/velocity/plugins/kishax -name "config.yml" 2>/dev/null | while read file; do
  if [ -f "$file" ]; then
    sed -i.bak "s|\${MYSQL_HOST}|${MYSQL_HOST:-mysql}|g" "$file"
    sed -i.bak "s|\${MYSQL_DATABASE}|${MYSQL_DATABASE:-mc}|g" "$file"
    sed -i.bak "s|\${MYSQL_PORT}|${MYSQL_PORT:-3306}|g" "$file"
    sed -i.bak "s|\${MYSQL_USER}|${MYSQL_USER:-root}|g" "$file"
    sed -i.bak "s|\${MYSQL_PASSWORD}|${MYSQL_PASSWORD:-password}|g" "$file"
    rm -f "$file.bak"
    echo "Updated MySQL credentials in $file"
  fi
done

echo "Configuration completed!"
echo "Starting servers..."

# Start Proxies
for ((i=0; i<$ACTIVE_PROXY_COUNT; i++)); do
  PROXY_NAME_VAR="PROXY_${i}_NAME"
  PROXY_MEMORY_VAR="PROXY_${i}_MEMORY"
  PROXY_FILENAME_VAR="PROXY_${i}_FILENAME"
  
  PROXY_NAME="${!PROXY_NAME_VAR}"
  PROXY_MEMORY="${!PROXY_MEMORY_VAR}"
  PROXY_FILENAME="${!PROXY_FILENAME_VAR}"
  
  echo "Starting Proxy: $PROXY_NAME (Memory: $PROXY_MEMORY) in screen session '$PROXY_NAME'..."
  cd /mc/velocity
  screen -dmS "$PROXY_NAME" java -Xmx"$PROXY_MEMORY" -jar "$PROXY_FILENAME"
  sleep 5
done

# Wait for proxies to initialize
echo "Waiting for proxies to initialize..."
sleep 10

# Start Spigots
for ((i=0; i<$ACTIVE_SPIGOT_COUNT; i++)); do
  SPIGOT_NAME_VAR="SPIGOT_${i}_NAME"
  SPIGOT_MEMORY_VAR="SPIGOT_${i}_MEMORY"
  SPIGOT_FILENAME_VAR="SPIGOT_${i}_FILENAME"
  SPIGOT_PORT_VAR="SPIGOT_${i}_PORT"
  
  SPIGOT_NAME="${!SPIGOT_NAME_VAR}"
  SPIGOT_MEMORY="${!SPIGOT_MEMORY_VAR}"
  SPIGOT_FILENAME="${!SPIGOT_FILENAME_VAR}"
  SPIGOT_PORT="${!SPIGOT_PORT_VAR}"
  
  # Check if this server needs S3 world data import
  S3IMPORT=$(jq -r ".spigots[$i].s3import // false" "$CONFIG_FILE")
  AUTO_START=$(jq -r ".spigots[$i].auto_start // true" "$CONFIG_FILE")
  
  # Skip auto_start=false servers
  if [ "$AUTO_START" = "false" ]; then
    echo "⏭️  Skipping $SPIGOT_NAME (auto_start=false)"
    continue
  fi
  
  # Import world data from S3 if enabled
  if [ "$S3IMPORT" = "true" ]; then
    echo ""
    echo "🌍 S3 import enabled for $SPIGOT_NAME, checking for world data..."
    /mc/scripts/import-world-from-s3.sh "$SPIGOT_NAME" || true
    echo ""
  fi
  
  echo "Starting Spigot: $SPIGOT_NAME (Memory: $SPIGOT_MEMORY, Port: $SPIGOT_PORT) in screen session '$SPIGOT_NAME'..."
  cd /mc/spigot
  screen -dmS "$SPIGOT_NAME" java -Xmx"$SPIGOT_MEMORY" -jar "$SPIGOT_FILENAME" --nogui
  sleep 3
done

# Keep container running by attaching to spigot screen
echo "Servers started! Use 'docker exec -it kishax-minecraft screen -r <session-name>' to access server consoles."
echo "Available screen sessions:"
screen -list

# Keep container alive by waiting for screen sessions
while screen -list | grep -qE "(home|latest|proxy|\.\.)" 2>/dev/null; do
  sleep 30
done

echo "All screen sessions have ended. Container will exit."
