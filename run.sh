#!/bin/bash

# Run API Server Script
# This script loads environment variables from .env and starts the API server

set -e  # Exit on error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "📁 Working directory: $SCRIPT_DIR"
echo "🔧 Loading environment variables from .env..."

# Load environment variables from .env
set -a
source "$SCRIPT_DIR/.env"
set +a

echo "✅ Environment variables loaded"
echo "🚀 Starting API server..."
echo ""

# Run the API
cd "$SCRIPT_DIR"
sbt "api/run"
