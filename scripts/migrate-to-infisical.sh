#!/bin/bash

# Migration script: .env files → Infisical
# Usage: ./migrate-to-infisical.sh <environment> <env-file>
# Example: ./migrate-to-infisical.sh development .env.security.example

set -e

ENVIRONMENT=${1:-development}
ENV_FILE=${2:-.env.security.example}

echo "🔐 Migrating secrets from $ENV_FILE to Infisical environment: $ENVIRONMENT"
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

# Check if env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Error: File $ENV_FILE not found"
    exit 1
fi

echo "✅ Infisical CLI found and authenticated"
echo ""

# Parse and upload secrets
COUNTER=0
SKIPPED=0

while IFS='=' read -r key value; do
    # Skip comments and empty lines
    if [[ $key =~ ^#.*$ ]] || [[ -z "$key" ]]; then
        continue
    fi
    
    # Trim whitespace
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    
    # Skip if value is empty or placeholder
    if [[ -z "$value" ]] || [[ "$value" == "your-"* ]] || [[ "$value" == "<"*">" ]]; then
        echo "⏭️  Skipping placeholder: $key"
        ((SKIPPED++))
        continue
    fi
    
    # Upload to Infisical
    echo "📤 Uploading: $key"
    if infisical secrets set "$key" "$value" --env="$ENVIRONMENT" --silent 2>/dev/null; then
        ((COUNTER++))
    else
        echo "⚠️  Warning: Failed to upload $key"
    fi
done < "$ENV_FILE"

echo ""
echo "✅ Migration complete!"
echo "   - Uploaded: $COUNTER secrets"
echo "   - Skipped: $SKIPPED placeholders"
echo ""

# Verify migration
echo "🔍 Verifying migration..."
echo ""
infisical secrets list --env="$ENVIRONMENT"

echo ""
echo "📝 Next steps:"
echo "1. Review secrets in Infisical UI: https://app.infisical.com"
echo "2. Update placeholder values with real secrets"
echo "3. Test with: infisical run --env=$ENVIRONMENT -- sbt compile"
echo "4. Delete local $ENV_FILE file after confirming everything works"
echo ""
echo "⚠️  IMPORTANT: For production, generate new strong secrets:"
echo "   infisical secrets set JWT_SECRET \"\$(openssl rand -base64 64)\" --env=production"
echo "   infisical secrets set DB_ENCRYPTION_KEY \"\$(openssl rand -base64 32)\" --env=production"
echo "   infisical secrets set SESSION_SECRET \"\$(openssl rand -base64 32)\" --env=production"
