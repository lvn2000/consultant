#!/bin/bash

# Run frontend apps with HTTPS using nginx reverse proxy
# Frontend apps run on HTTP, nginx provides HTTPS layer

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CERTS_DIR="$SCRIPT_DIR/certs"
CERT_FILE="$CERTS_DIR/certificate.crt"
KEY_FILE="$CERTS_DIR/private.key"

# Check if certificates exist
if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then
    echo "⚠️  SSL certificates not found!"
    echo "Run ./start-https.sh first to generate certificates."
    exit 1
fi

# Show usage
if [ -z "$1" ]; then
    echo "Usage: $0 <app-name>"
    echo ""
    echo "Available apps:"
    echo "  admin       - Admin app (https://localhost:3443)"
    echo "  client      - Client app (https://localhost:3444)"
    echo "  specialist  - Specialist app (https://localhost:3445)"
    echo "  all         - All three apps"
    echo ""
    echo "Examples:"
    echo "  $0 admin"
    echo "  $0 specialist"
    echo "  $0 all"
    exit 0
fi

# Check if nginx is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is required but not installed."
    exit 1
fi

run_app() {
    local app_name=$1
    local app_dir=$2
    local http_port=$3
    local https_port=$4

    echo "🚀 Starting $app_name..."
    echo "   HTTP (internal): http://localhost:$http_port"
    echo "   HTTPS (browser): https://localhost:$https_port"
    echo ""

    cd "$SCRIPT_DIR/$app_dir"

    # Clear cache
    rm -rf .nuxt .output 2>/dev/null || true

    # Start the app in background
    NUXT_HOST="0.0.0.0" \
    NUXT_PORT="$http_port" \
    npm run dev > /tmp/$app_dir.log 2>&1 &

    local pid=$!
    echo $pid > /tmp/$app_dir.pid
    echo "   Process ID: $pid"

    # Wait for app to start
    sleep 5

    # Check if process is still running
    if ! kill -0 $pid 2>/dev/null; then
        echo "❌ Failed to start $app_name. Check logs:"
        cat /tmp/$app_dir.log
        return 1
    fi

    echo "✅ $app_name started on HTTP port $http_port"
}

start_nginx() {
    echo ""
    echo "🔒 Starting nginx HTTPS proxy..."

    # Stop existing container
    docker rm -f consultant-frontend-nginx 2>/dev/null || true

    # Start nginx with HTTPS
    docker run -d \
        --name consultant-frontend-nginx \
        --network host \
        -v "$SCRIPT_DIR/nginx-frontend-https.conf:/etc/nginx/nginx.conf:ro" \
        -v "$CERTS_DIR:/etc/nginx/certs:ro" \
        nginx:alpine

    echo "✅ nginx started"
}

stop_apps() {
    echo "🛑 Stopping frontend apps..."

    # Stop nginx
    docker rm -f consultant-frontend-nginx 2>/dev/null || true

    # Stop node processes
    for app_dir in admin-app client-app specialist-app; do
        if [ -f "/tmp/$app_dir.pid" ]; then
            local pid=$(cat /tmp/$app_dir.pid)
            if kill -0 $pid 2>/dev/null; then
                kill $pid 2>/dev/null || true
                echo "   Stopped $app_dir (PID: $pid)"
            fi
            rm -f /tmp/$app_dir.pid
        fi
    done

    echo "✅ All stopped"
}

# Handle stop command
if [ "$1" = "stop" ]; then
    stop_apps
    exit 0
fi

case "$1" in
    admin)
        run_app "Admin App" "admin-app" "3000" "3443"
        start_nginx
        echo ""
        echo "✅ Admin app available at: https://localhost:3443"
        ;;
    client)
        run_app "Client App" "client-app" "3001" "3444"
        start_nginx
        echo ""
        echo "✅ Client app available at: https://localhost:3444"
        ;;
    specialist)
        run_app "Specialist App" "specialist-app" "3003" "3445"
        start_nginx
        echo ""
        echo "✅ Specialist app available at: https://localhost:3445"
        ;;
    all)
        echo "🚀 Starting all frontend apps with HTTPS..."
        echo ""

        # Start all apps
        run_app "Admin App" "admin-app" "3000" "3443"
        run_app "Client App" "client-app" "3001" "3444"
        run_app "Specialist App" "specialist-app" "3003" "3445"

        start_nginx

        echo ""
        echo "=========================================="
        echo "✅ All frontend apps started with HTTPS!"
        echo "=========================================="
        echo ""
        echo "Access points:"
        echo "  Admin:       https://localhost:3443"
        echo "  Client:      https://localhost:3444"
        echo "  Specialist:  https://localhost:3445"
        echo ""
        echo "To stop: $0 stop"
        echo ""
        ;;
    *)
        echo "❌ Unknown app: $1"
        echo "Run '$0' without arguments to see available options."
        exit 1
        ;;
esac
