#!/bin/bash

# Run API Server Script
# This script:
# 1. Checks for existing backend process and kills it if running
# 2. Starts PostgreSQL in Docker (if not running)
# 3. Loads environment variables from .env
# 4. Starts the API server

set -e  # Exit on error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "📁 Working directory: $SCRIPT_DIR"
echo ""

# Step 0: Check for existing backend process and kill if running
echo "🔍 Checking for existing backend process..."
cd "$SCRIPT_DIR"

# Load .env to get SERVER_PORT (with defaults)
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
fi

# Default port if not set
SERVER_PORT="${SERVER_PORT:-8090}"

# Check if port is in use
if command -v lsof &> /dev/null; then
    EXISTING_PID=$(lsof -ti:$SERVER_PORT 2>/dev/null || true)
elif command -v fuser &> /dev/null; then
    EXISTING_PID=$(fuser -t $SERVER_PORT/tcp 2>/dev/null | cut -d'/' -f2 || true)
elif command -v ss &> /dev/null; then
    EXISTING_PID=$(ss -tlnp 2>/dev/null | grep ":$SERVER_PORT " | grep -oP 'pid=\K[0-9]+' || true)
elif command -v netstat &> /dev/null; then
    EXISTING_PID=$(netstat -tlnp 2>/dev/null | grep ":$SERVER_PORT " | awk '{print $7}' | cut -d'/' -f1 || true)
else
    EXISTING_PID=""
fi

if [ -n "$EXISTING_PID" ]; then
    echo "⚠️  Found existing backend process on port $SERVER_PORT (PID: $EXISTING_PID)"
    echo "🛑 Stopping existing backend process..."

    # Try graceful shutdown first
    kill $EXISTING_PID 2>/dev/null || true
    sleep 2

    # Force kill if still running
    if kill -0 $EXISTING_PID 2>/dev/null; then
        echo "⚠️  Process still running, force killing..."
        kill -9 $EXISTING_PID 2>/dev/null || true
        sleep 1
    fi

    echo "✅ Existing backend process stopped"
else
    echo "✅ No existing backend process found on port $SERVER_PORT"
fi

# Also kill any sbt processes related to api/run
SBT_PID=$(pgrep -f "sbt.*api/run" 2>/dev/null || true)
if [ -n "$SBT_PID" ]; then
    echo "⚠️  Found sbt process (PID: $SBT_PID), stopping..."
    kill $SBT_PID 2>/dev/null || true
    sleep 2
    kill -9 $SBT_PID 2>/dev/null || true
    echo "✅ sbt process stopped"
fi

echo ""

# Step 1: Start Docker containers
echo "🐳 Starting Docker containers..."
cd "$SCRIPT_DIR"

# Check if containers are already running
if docker ps | grep -q consultant-db-master; then
    echo "✅ PostgreSQL container already running"
else
    echo "🚀 Starting PostgreSQL..."
    docker compose up -d

    # Wait for PostgreSQL to be ready
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 3

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
