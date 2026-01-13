#!/bin/bash

# Setup all required secrets in Infisical for all environments
# Usage: ./setup-infisical-secrets.sh

set -e

echo "🔐 Setting up Infisical secrets for Consultant Backend"
echo ""

# Check if Infisical CLI is installed
if ! command -v infisical &> /dev/null; then
    echo "❌ Error: Infisical CLI not found"
    echo "Install: brew install infisical/get-cli/infisical"
    exit 1
fi

# Check if user is logged in
if ! infisical whoami &> /dev/null; then
    echo "❌ Error: Not logged in to Infisical"
    echo "Run: infisical login"
    exit 1
fi

echo "✅ Infisical CLI found and authenticated"
echo ""

# Function to generate strong secret
generate_secret() {
    local length=${1:-32}
    openssl rand -base64 "$length" | tr -d '\n'
}

# Function to set secret if not exists
set_if_not_exists() {
    local key=$1
    local value=$2
    local env=$3
    
    # Check if secret already exists
    if infisical secrets get "$key" --env="$env" --silent 2>/dev/null; then
        echo "⏭️  $key already exists in $env (skipping)"
    else
        echo "📤 Creating $key in $env"
        infisical secrets set "$key" "$value" --env="$env" --silent
    fi
}

# ==================== DEVELOPMENT ENVIRONMENT ====================
echo "🔧 Setting up DEVELOPMENT environment..."
echo ""

set_if_not_exists "JWT_SECRET" "$(generate_secret 64)" "development"
set_if_not_exists "JWT_ISSUER" "consultant-api" "development"
set_if_not_exists "JWT_ACCESS_TTL_MINUTES" "15" "development"
set_if_not_exists "JWT_REFRESH_TTL_DAYS" "7" "development"

set_if_not_exists "DB_HOST" "localhost" "development"
set_if_not_exists "DB_PORT" "5432" "development"
set_if_not_exists "DB_NAME" "consultant_dev" "development"
set_if_not_exists "DB_USER" "postgres" "development"
set_if_not_exists "DB_PASSWORD" "postgres" "development"
set_if_not_exists "DB_ENCRYPTION_KEY" "$(generate_secret 32)" "development"

set_if_not_exists "REDIS_HOST" "localhost" "development"
set_if_not_exists "REDIS_PORT" "6379" "development"
set_if_not_exists "REDIS_PASSWORD" "" "development"

set_if_not_exists "SESSION_SECRET" "$(generate_secret 32)" "development"
set_if_not_exists "ACCOUNT_LOCK_DURATION_MINUTES" "15" "development"
set_if_not_exists "MAX_LOGIN_ATTEMPTS" "5" "development"

set_if_not_exists "CORS_ALLOWED_ORIGINS" "http://localhost:3000,http://localhost:8080" "development"
set_if_not_exists "FORCE_HTTPS" "false" "development"
set_if_not_exists "SECURE_COOKIES" "false" "development"

echo ""
echo "✅ Development environment configured"
echo ""

# ==================== STAGING ENVIRONMENT ====================
echo "🔧 Setting up STAGING environment..."
echo ""

set_if_not_exists "JWT_SECRET" "$(generate_secret 64)" "staging"
set_if_not_exists "JWT_ISSUER" "consultant-api" "staging"
set_if_not_exists "JWT_ACCESS_TTL_MINUTES" "15" "staging"
set_if_not_exists "JWT_REFRESH_TTL_DAYS" "7" "staging"

set_if_not_exists "DB_HOST" "staging-db.example.com" "staging"
set_if_not_exists "DB_PORT" "5432" "staging"
set_if_not_exists "DB_NAME" "consultant_staging" "staging"
set_if_not_exists "DB_USER" "consultant" "staging"
set_if_not_exists "DB_PASSWORD" "$(generate_secret 32)" "staging"
set_if_not_exists "DB_ENCRYPTION_KEY" "$(generate_secret 32)" "staging"

set_if_not_exists "REDIS_HOST" "staging-redis.example.com" "staging"
set_if_not_exists "REDIS_PORT" "6379" "staging"
set_if_not_exists "REDIS_PASSWORD" "$(generate_secret 24)" "staging"

set_if_not_exists "SESSION_SECRET" "$(generate_secret 32)" "staging"
set_if_not_exists "ACCOUNT_LOCK_DURATION_MINUTES" "15" "staging"
set_if_not_exists "MAX_LOGIN_ATTEMPTS" "5" "staging"

set_if_not_exists "CORS_ALLOWED_ORIGINS" "https://staging.yourdomain.com" "staging"
set_if_not_exists "FORCE_HTTPS" "true" "staging"
set_if_not_exists "SECURE_COOKIES" "true" "staging"

echo ""
echo "✅ Staging environment configured"
echo ""

# ==================== PRODUCTION ENVIRONMENT ====================
echo "🔧 Setting up PRODUCTION environment..."
echo ""

set_if_not_exists "JWT_SECRET" "$(generate_secret 64)" "production"
set_if_not_exists "JWT_ISSUER" "consultant-api" "production"
set_if_not_exists "JWT_ACCESS_TTL_MINUTES" "15" "production"
set_if_not_exists "JWT_REFRESH_TTL_DAYS" "7" "production"

set_if_not_exists "DB_HOST" "prod-db.example.com" "production"
set_if_not_exists "DB_PORT" "5432" "production"
set_if_not_exists "DB_NAME" "consultant_prod" "production"
set_if_not_exists "DB_USER" "consultant" "production"
set_if_not_exists "DB_PASSWORD" "$(generate_secret 32)" "production"
set_if_not_exists "DB_ENCRYPTION_KEY" "$(generate_secret 32)" "production"

set_if_not_exists "REDIS_HOST" "prod-redis.example.com" "production"
set_if_not_exists "REDIS_PORT" "6379" "production"
set_if_not_exists "REDIS_PASSWORD" "$(generate_secret 24)" "production"

set_if_not_exists "SESSION_SECRET" "$(generate_secret 32)" "production"
set_if_not_exists "ACCOUNT_LOCK_DURATION_MINUTES" "15" "production"
set_if_not_exists "MAX_LOGIN_ATTEMPTS" "5" "production"

set_if_not_exists "CORS_ALLOWED_ORIGINS" "https://yourdomain.com,https://app.yourdomain.com" "production"
set_if_not_exists "FORCE_HTTPS" "true" "production"
set_if_not_exists "SECURE_COOKIES" "true" "production"

echo ""
echo "✅ Production environment configured"
echo ""

# ==================== SUMMARY ====================
echo "📊 Summary"
echo ""
echo "Development secrets:"
infisical secrets list --env=development | grep -E "JWT_SECRET|DB_PASSWORD|DB_ENCRYPTION_KEY" || true
echo ""
echo "Staging secrets:"
infisical secrets list --env=staging | grep -E "JWT_SECRET|DB_PASSWORD|DB_ENCRYPTION_KEY" || true
echo ""
echo "Production secrets:"
infisical secrets list --env=production | grep -E "JWT_SECRET|DB_PASSWORD|DB_ENCRYPTION_KEY" || true
echo ""

echo "✅ All environments configured!"
echo ""
echo "📝 Next steps:"
echo "1. Review secrets in Infisical UI"
echo "2. Update DB hosts and credentials with real values"
echo "3. Test locally: infisical run --env=development -- sbt run"
echo "4. Create service token for Docker: infisical service-token --env=production"
echo "5. Create service token for K8s: infisical service-token --env=production --scope=read"
echo ""
echo "⚠️  IMPORTANT: Backup these secrets securely!"
echo "   infisical secrets export --env=production --format=json > backup-\$(date +%Y%m%d).json"
