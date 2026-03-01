#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "🛑 Stopping HTTPS Stack..."

echo "  - Stopping Nginx..."
docker rm -f consultant-nginx 2>/dev/null || echo "  - consultant-nginx not running"

echo "  - Stopping API instances..."
for i in 1 2 3; do
  docker rm -f consultant-app-$i 2>/dev/null || echo "  - consultant-app-$i not running"
done

echo "  - Stopping PostgreSQL..."
docker rm -f consultant-db 2>/dev/null || echo "  - consultant-db not running"

echo ""
echo "✅ All services stopped."
echo ""
echo "To remove the Docker network, run:"
echo "   docker network rm consultant-net"
echo ""
