#!/bin/bash

# Run API Server Script
# This script:
# 1. Starts PostgreSQL & Redis in Docker (if not running)
# 2. Loads environment variables from .env
# 3. Starts the API server

set -e  # Exit on error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "📁 Working directory: $SCRIPT_DIR"
echo ""

# Step 1: Start Docker containers
echo "🐳 Starting Docker containers..."
cd "$SCRIPT_DIR"

# Check if containers are already running
if docker ps | grep -q consultant-db-master && docker ps | grep -q consultant-redis; then
    echo "✅ Containers already running"
else
    echo "🚀 Starting PostgreSQL and Redis..."
    docker-compose up -d
    
    # Wait for PostgreSQL to be ready
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 5
    
    # Check health
    for i in {1..30}; do
        if docker exec consultant-db-master pg_isready -U consultant_user > /dev/null 2>&1; then
            echo "✅ PostgreSQL is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "❌ PostgreSQL failed to start"
            exit 1
        fi
        sleep 1
    done
fi

echo ""

# Step 2: Load environment variables
echo "🔧 Loading environment variables from .env..."

# Load environment variables from .env
set -a
source "$SCRIPT_DIR/.env"
set +a

echo "✅ Environment variables loaded"
echo "📊 Database: $DB_URL"
echo "👤 User: $DB_USER"
echo ""

# Step 3: Start API server
echo "🚀 Starting API server..."
echo ""

# Run the API
cd "$SCRIPT_DIR"
sbt "api/run"
