#!/bin/bash
set -e

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
echo "DEBUG: AWS Environment Variables:"
echo "AWS_REGION=${AWS_REGION}"
echo "MC_WEB_SQS_ACCESS_KEY_ID=${MC_WEB_SQS_ACCESS_KEY_ID:0:10}..."           # 最初の10文字のみ表示
echo "MC_WEB_SQS_SECRET_ACCESS_KEY=${MC_WEB_SQS_SECRET_ACCESS_KEY:0:10}..."   # 最初の10文字のみ表示
echo "DISCORD_API_ACCESS_KEY_ID=${DISCORD_API_ACCESS_KEY_ID:0:10}..."         # 最初の10文字のみ表示
echo "DISCORD_API_SECRET_ACCESS_KEY=${DISCORD_API_SECRET_ACCESS_KEY:0:10}..." # 最初の10文字のみ表示
echo "WEB_TO_MC_QUEUE_URL=${WEB_TO_MC_QUEUE_URL}"
echo "MC_TO_WEB_QUEUE_URL=${MC_TO_WEB_QUEUE_URL}"
echo "API_GATEWAY_URL=${API_GATEWAY_URL}"
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
  # Discord API Configuration replacements
  sed -i.bak "s|\${DISCORD_API_ACCESS_KEY_ID}|${DISCORD_API_ACCESS_KEY_ID}|g" "$file"
  sed -i.bak "s|\${DISCORD_API_SECRET_ACCESS_KEY}|${DISCORD_API_SECRET_ACCESS_KEY}|g" "$file"
  # Queue URLs and API Gateway
  sed -i.bak "s|\${WEB_TO_MC_QUEUE_URL}|${WEB_TO_MC_QUEUE_URL}|g" "$file"
  sed -i.bak "s|\${MC_TO_WEB_QUEUE_URL}|${MC_TO_WEB_QUEUE_URL}|g" "$file"
  sed -i.bak "s|\${API_GATEWAY_URL}|${API_GATEWAY_URL}|g" "$file"
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

# Final verification
VELOCITY_SECRET=$(cat /mc/velocity/forwarding.secret)
PAPER_SECRET=$(grep "secret:" /mc/spigot/config/paper-global.yml | head -1 | awk '{print $2}')
echo "Final check - Velocity: $VELOCITY_SECRET, Paper: $PAPER_SECRET"
if [ "$VELOCITY_SECRET" = "$PAPER_SECRET" ]; then
  echo "✅ Forwarding secrets match!"
else
  echo "❌ Forwarding secrets do NOT match!"
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

# Start Velocity proxy in screen session
echo "Starting Velocity proxy in screen session 'velocity'..."
cd /mc/velocity
screen -dmS velocity java -Xmx${VELOCITY_MEMORY:-1G} -jar velocity-*.jar

# Wait a moment for Velocity to start
echo "Waiting for Velocity to initialize..."
sleep 10

# Start Paper server in screen session
echo "Starting Paper server in screen session 'spigot'..."
cd /mc/spigot
screen -dmS spigot java -Xmx${SPIGOT_MEMORY:-2G} -jar paper-*.jar --nogui

# Keep container running by attaching to spigot screen
echo "Servers started! Use 'make mc-spigot' or 'make mc-velocity' to access server consoles."
echo "Available screen sessions:"
screen -list

# Keep container alive by waiting for screen sessions
while screen -list | grep -q "spigot\|velocity"; do
  sleep 30
done

