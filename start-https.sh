#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "📦 Creating Docker network..."
docker network create consultant-net 2>/dev/null || echo "Network already exists"

echo "🔨 Building API image (JDK 21)..."
docker build -t consultant-api:latest .

echo "📊 Starting PostgreSQL..."
docker rm -f consultant-db 2>/dev/null || true
docker run -d \
  --name consultant-db \
  --network consultant-net \
  -e POSTGRES_DB=consultant \
  -e POSTGRES_USER=consultant_user \
  -e POSTGRES_PASSWORD=consultant_pass \
  -p 5432:5432 \
  --health-cmd="pg_isready -U consultant_user" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=5 \
  postgres:16-alpine

echo "🔴 Starting Redis..."
docker rm -f consultant-redis 2>/dev/null || true
docker run -d \
  --name consultant-redis \
  --network consultant-net \
  -p 6379:6379 \
  --health-cmd="redis-cli ping" \
  --health-interval=10s \
  --health-timeout=3s \
  --health-retries=5 \
  redis:7-alpine

echo "⏳ Waiting for database and cache to be ready (30s)..."
sleep 30

echo "🚀 Starting API instances..."
for i in 1 2 3; do
  PORT=$((8080 + i))
  echo "  - Starting app-$i on port $PORT..."
  docker rm -f consultant-app-$i 2>/dev/null || true
  docker run -d \
    --name consultant-app-$i \
    --network consultant-net \
    -e DB_HOST=consultant-db \
    -e DB_PORT=5432 \
    -e DB_NAME=consultant \
    -e DB_USER=consultant_user \
    -e DB_PASSWORD=consultant_pass \
    -e DB_ENCRYPTION_KEY="dev-encryption-key-change-in-prod" \
    -e REDIS_HOST=consultant-redis \
    -e REDIS_PORT=6379 \
    -e SERVER_HOST=0.0.0.0 \
    -e SERVER_PORT=8090 \
    -e JWT_SECRET="dev-jwt-secret-change-in-production-minimum-64-characters-long" \
    -e JWT_ISSUER="consultant-api" \
    -e JWT_ACCESS_TTL_MINUTES=15 \
    -e JWT_REFRESH_TTL_DAYS=7 \
    -e SESSION_SECRET="dev-session-secret" \
    -e MAX_LOGIN_ATTEMPTS=5 \
    -e ACCOUNT_LOCK_DURATION_MINUTES=15 \
    -e FORCE_HTTPS=true \
    -e SECURE_COOKIES=true \
    -e SESSION_SECURE=true \
    -p $PORT:8090 \
    consultant-api:latest
done

echo "⏳ Waiting for API instances to be ready (15s)..."
sleep 15

echo "🔒 Starting Nginx with HTTPS..."
docker rm -f consultant-nginx 2>/dev/null || true
docker run -d \
  --name consultant-nginx \
  --network consultant-net \
  -p 9080:80 \
  -p 9443:443 \
  -v "$(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro" \
  -v "$(pwd)/certs:/etc/nginx/certs:ro" \
  --health-cmd="wget --quiet --tries=1 --spider https://localhost/health || wget --quiet --tries=1 --spider http://localhost/health || exit 1" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  nginx:alpine

echo ""
echo "✅ HTTPS Stack Started Successfully!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Access Points:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🌐 HTTP (auto-redirects to HTTPS)"
echo "   URL: http://localhost:9080"
echo ""
echo "🔒 HTTPS (Secure)"
echo "   URL: https://localhost:9443"
echo "   Note: Use -k flag with curl for self-signed cert"
echo ""
echo "📱 Internal Service Endpoints:"
echo "   App-1: http://localhost:8081/health"
echo "   App-2: http://localhost:8082/health"
echo "   App-3: http://localhost:8083/health"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Test Commands:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Test HTTPS:"
echo "   curl -k https://localhost:9443/health"
echo ""
echo "Test HTTP Redirect:"
echo "   curl -I http://localhost:9080/"
echo ""
echo "Test Security Headers:"
echo "   curl -k https://localhost:9443 -I | grep Strict"
echo ""
echo "View Logs:"
echo "   docker logs consultant-nginx"
echo "   docker logs consultant-app-1"
echo ""
echo "Stop All Services:"
echo "   docker-compose down  # or manually stop containers"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
