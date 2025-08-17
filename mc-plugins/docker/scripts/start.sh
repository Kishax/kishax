#!/bin/bash
set -e

echo "Starting Kishax Minecraft Server Environment..."

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
while ! mysql -h${MYSQL_HOST:-mysql} -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-password} -e "SELECT 1" > /dev/null 2>&1; do
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
        mysql -h${MYSQL_HOST:-mysql} -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-password} ${MYSQL_DATABASE:-mc} < "$sql_file"
    fi
done

# Generate random secret for Velocity forwarding
FORWARDING_SECRET=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9' | head -c 12)
echo "Generated forwarding secret: $FORWARDING_SECRET"

# Create forwarding.secret file for Velocity
echo "$FORWARDING_SECRET" > /mc/velocity/forwarding.secret
echo "Created forwarding.secret file for Velocity"

# Debug: Show initial forwarding.secret content
echo "DEBUG: Initial forwarding.secret content:"
cat /mc/velocity/forwarding.secret

# Debug: Show initial paper-global.yml content for velocity section
echo "DEBUG: Initial paper-global.yml velocity section:"
grep -A5 -B5 "velocity:" /mc/spigot/config/paper-global.yml || echo "paper-global.yml not found or no velocity section"

# Replace placeholders in config files
echo "Configuring server files..."
find /mc -type f \( -name "*.yml" -o -name "*.toml" -o -name "forwarding.secret" \) | while read file; do
    echo "DEBUG: Processing file: $file"
    
    # Show content before replacement for critical files
    if [[ "$file" == *"forwarding.secret"* ]] || [[ "$file" == *"paper-global.yml"* ]]; then
        echo "DEBUG: Content before replacement in $file:"
        cat "$file"
    fi
    
    sed -i.bak "s|\${THIS_IS_SECRET}|${FORWARDING_SECRET}|g" "$file"
    sed -i.bak "s|\${MYSQL_HOST}|${MYSQL_HOST:-mysql}|g" "$file"
    sed -i.bak "s|\${MYSQL_DATABASE}|${MYSQL_DATABASE:-mc}|g" "$file"
    sed -i.bak "s|\${MYSQL_PORT}|${MYSQL_PORT:-3306}|g" "$file"
    sed -i.bak "s|\${MYSQL_USER}|${MYSQL_USER:-root}|g" "$file"
    sed -i.bak "s|\${MYSQL_PASSWORD}|${MYSQL_PASSWORD:-password}|g" "$file"
    sed -i.bak "s|\${CONFIRM_URL:-https://your-confirm-url.com}|${CONFIRM_URL:-https://your-confirm-url.com}|g" "$file"
    sed -i.bak "s|\${HOME_SERVER_NAME:-spigot}|${HOME_SERVER_NAME:-spigot}|g" "$file"
    sed -i.bak "s|\${HOME_SERVER_IP:-127.0.0.1}|${HOME_SERVER_IP:-127.0.0.1}|g" "$file"
    rm -f "$file.bak"
    
    # Show content after replacement for critical files
    if [[ "$file" == *"forwarding.secret"* ]] || [[ "$file" == *"paper-global.yml"* ]]; then
        echo "DEBUG: Content after replacement in $file:"
        cat "$file"
    fi
    
    echo "Updated placeholders in $file"
done

# Final verification: Show the actual secret values being used
echo "DEBUG: Final verification of forwarding secrets:"
echo "Velocity forwarding.secret:"
cat /mc/velocity/forwarding.secret
echo ""
echo "Paper velocity secret in paper-global.yml:"
grep "secret:" /mc/spigot/config/paper-global.yml || echo "No secret found in paper-global.yml"

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

# Start Velocity proxy in background
echo "Starting Velocity proxy..."
cd /mc/velocity
java -Xmx${VELOCITY_MEMORY:-1G} -jar velocity-*.jar &
VELOCITY_PID=$!

# Wait a moment for Velocity to start
echo "Waiting for Velocity to initialize..."
sleep 10

# Start Paper server (foreground)
echo "Starting Paper server..."
cd /mc/spigot
exec java -Xmx${SPIGOT_MEMORY:-2G} -jar paper-*.jar --nogui