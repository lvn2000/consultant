# HTTPS Implementation Status & Manual Setup

## ✅ Completed Tasks

### 1. **JDK 21 Dockerfile Update**
- ✅ Updated build stage to use `eclipse-temurin:21-jdk-jammy`
- ✅ Updated runtime stage to use `eclipse-temurin:21-jre-jammy`
- ✅ Fixed Scala version reference to 3.4.2
- ✅ Installed SBT 1.9.8 in build stage

### 2. **HTTPS Configuration**
- ✅ Updated nginx.conf with:
  - HTTP → HTTPS redirect
  - TLS 1.2 & 1.3 support
  - Security headers (HSTS, CSP, etc.)
  - HTTP/2 support
- ✅ Updated docker-compose.yml with:
  - HTTPS port mapping (9443)
  - Certificate volume mount
  - FORCE_HTTPS=true environment variables
  - SECURE_COOKIES=true

### 3. **SSL Certificates**
- ✅ Generated self-signed certificates (365 days valid)
- ✅ Located at: `./certs/certificate.crt` and `./certs/private.key`
- ✅ Created certificate generation script

### 4. **Documentation**
- ✅ Created HTTPS_QUICKSTART.md
- ✅ Created certificate generation script

---

## 🚀 Manual HTTPS Startup (Workaround for docker-compose issue)

### Step 1: Build Docker Images
```bash
cd /home/lvn/prg/scala/Consultant/backend

# Build API application image
docker build -t consultant-api:latest .
```

### Step 2: Start Backend Services
```bash
# Start PostgreSQL
docker run -d \
  --name consultant-db \
  --network consultant-net \
  -e POSTGRES_DB=consultant \
  -e POSTGRES_USER=consultant_user \
  -e POSTGRES_PASSWORD=consultant_pass \
  -p 5432:5432 \
  postgres:16-alpine

# Start Redis
docker run -d \
  --name consultant-redis \
  --network consultant-net \
  -p 6379:6379 \
  redis:7-alpine

# Wait for services to be ready
sleep 10
```

### Step 3: Start Application Instances
```bash
# Start app-1
docker run -d \
  --name consultant-app-1 \
  --network consultant-net \
  -e DB_HOST=consultant-db \
  -e DB_PORT=5432 \
  -e DB_NAME=consultant \
  -e DB_USER=consultant_user \
  -e DB_PASSWORD=consultant_pass \
  -e REDIS_HOST=consultant-redis \
  -e REDIS_PORT=6379 \
  -e SERVER_HOST=0.0.0.0 \
  -e SERVER_PORT=8090 \
  -e FORCE_HTTPS=true \
  -e SECURE_COOKIES=true \
  -e SESSION_SECURE=true \
  -p 8081:8090 \
  consultant-api:latest

# Start app-2
docker run -d \
  --name consultant-app-2 \
  --network consultant-net \
  -e DB_HOST=consultant-db \
  -e DB_PORT=5432 \
  -e DB_NAME=consultant \
  -e DB_USER=consultant_user \
  -e DB_PASSWORD=consultant_pass \
  -e REDIS_HOST=consultant-redis \
  -e REDIS_PORT=6379 \
  -e SERVER_HOST=0.0.0.0 \
  -e SERVER_PORT=8090 \
  -e FORCE_HTTPS=true \
  -e SECURE_COOKIES=true \
  -e SESSION_SECURE=true \
  -p 8082:8090 \
  consultant-api:latest

# Start app-3
docker run -d \
  --name consultant-app-3 \
  --network consultant-net \
  -e DB_HOST=consultant-db \
  -e DB_PORT=5432 \
  -e DB_NAME=consultant \
  -e DB_USER=consultant_user \
  -e DB_PASSWORD=consultant_pass \
  -e REDIS_HOST=consultant-redis \
  -e REDIS_PORT=6379 \
  -e SERVER_HOST=0.0.0.0 \
  -e SERVER_PORT=8090 \
  -e FORCE_HTTPS=true \
  -e SECURE_COOKIES=true \
  -e SESSION_SECURE=true \
  -p 8083:8090 \
  consultant-api:latest

# Wait for apps to be ready
sleep 10
```

### Step 4: Start Nginx with HTTPS
```bash
# Start Nginx with HTTPS
docker run -d \
  --name consultant-nginx \
  --network consultant-net \
  -p 9080:80 \
  -p 9443:443 \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v $(pwd)/certs:/etc/nginx/certs:ro \
  nginx:alpine
```

### Step 5: Create Docker Network (if not exists)
```bash
# Create network first
docker network create consultant-net 2>/dev/null || true
```

### Complete Startup Script
Save this as `start-https.sh` and run with `bash start-https.sh`:

```bash
#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "Creating Docker network..."
docker network create consultant-net 2>/dev/null || true

echo "Building API image..."
docker build -t consultant-api:latest .

echo "Starting PostgreSQL..."
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
  postgres:16-alpine

echo "Starting Redis..."
docker run -d \
  --name consultant-redis \
  --network consultant-net \
  -p 6379:6379 \
  --health-cmd="redis-cli ping" \
  --health-interval=10s \
  --health-timeout=3s \
  redis:7-alpine

echo "Waiting for database to be ready..."
sleep 15

echo "Starting API instances..."
for i in 1 2 3; do
  PORT=$((8080 + i))
  docker run -d \
    --name consultant-app-$i \
    --network consultant-net \
    -e DB_HOST=consultant-db \
    -e DB_PORT=5432 \
    -e DB_NAME=consultant \
    -e DB_USER=consultant_user \
    -e DB_PASSWORD=consultant_pass \
    -e REDIS_HOST=consultant-redis \
    -e REDIS_PORT=6379 \
    -e SERVER_HOST=0.0.0.0 \
    -e SERVER_PORT=8090 \
    -e FORCE_HTTPS=true \
    -e SECURE_COOKIES=true \
    -e SESSION_SECURE=true \
    -p $PORT:8090 \
    consultant-api:latest
done

echo "Waiting for API instances to be ready..."
sleep 10

echo "Starting Nginx with HTTPS..."
docker run -d \
  --name consultant-nginx \
  --network consultant-net \
  -p 9080:80 \
  -p 9443:443 \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v $(pwd)/certs:/etc/nginx/certs:ro \
  nginx:alpine

echo ""
echo "✅ HTTPS Stack Started Successfully!"
echo ""
echo "Access points:"
echo "  HTTP (redirects to HTTPS):  http://localhost:9080"
echo "  HTTPS:                      https://localhost:9443"
echo ""
echo "Internal endpoints:"
echo "  App-1: http://localhost:8081/health"
echo "  App-2: http://localhost:8082/health"
echo "  App-3: http://localhost:8083/health"
echo ""
echo "Test HTTPS:"
echo "  curl -k https://localhost:9443/health"
echo ""
echo "Test HTTP redirect:"
echo "  curl -i http://localhost:9080/"
echo ""
```

---

## 🧪 Testing HTTPS

### Test 1: HTTPS Endpoint
```bash
curl -k https://localhost:9443/health
```

### Test 2: HTTP Redirect
```bash
curl -I http://localhost:9080/
# Should return: HTTP/1.1 301 Moved Permanently
# Location: https://localhost:9080/...
```

### Test 3: Security Headers
```bash
curl -k https://localhost:9443 -I | grep -i "Strict-Transport-Security"
# Should show: Strict-Transport-Security: max-age=31536000...
```

### Test 4: Browser Access
```
https://localhost:9443  (with -k flag for curl to ignore self-signed)
```

---

## 📋 Docker-Compose Issue

**Current Issue:** `KeyError: 'ContainerConfig'` when using docker-compose

**Cause:** Compatibility issue between docker-compose v1.29.2 and newer Docker versions.

**Solutions:**
1. **Use manual docker run commands** (recommended above)
2. **Upgrade to docker-compose v2:**
   ```bash
   sudo apt-get install docker-compose-plugin
   docker compose up -d  # Note: no dash
   ```
3. **Use the manual startup script** provided in this guide

---

## 🛑 Cleanup

```bash
# Stop all containers
docker stop consultant-nginx consultant-app-{1,2,3} consultant-redis consultant-db

# Remove all containers
docker rm consultant-nginx consultant-app-{1,2,3} consultant-redis consultant-db

# Remove network
docker network rm consultant-net

# Remove image
docker rmi consultant-api:latest
```

---

## 📊 Architecture

```
┌─────────────────────────────────────────┐
│   HTTPS Traffic (9443)                  │
│   HTTP  Traffic (9080)                  │
└────────────┬────────────────────────────┘
             │
             ▼
        ┌─────────────┐
        │   Nginx     │  (Load Balancer + HTTPS)
        │  + TLS 1.3  │
        └──┬──┬───┬───┘
           │  │   │
    ┌──────┘  │   └──────┐
    ▼         ▼          ▼
┌─────────┐ ┌──────────┐ ┌──────────┐
│ App-1   │ │  App-2   │ │  App-3   │
│ :8090   │ │  :8090   │ │  :8090   │
└────┬────┘ └────┬─────┘ └────┬─────┘
     └───────────┼──────────────┘
                 │
         ┌───────┴────────┐
         ▼                ▼
     ┌────────┐      ┌────────┐
     │   DB   │      │ Redis  │
     │ :5432  │      │ :6379  │
     └────────┘      └────────┘
```

---

## 🔐 HTTPS Features Enabled

✅ HTTP/2 Support  
✅ TLS 1.2 & 1.3  
✅ HSTS Header (1-year)  
✅ Secure Cookies  
✅ X-Frame-Options Protection  
✅ X-Content-Type-Options  
✅ CSP Headers  
✅ HTTP → HTTPS Redirect  

---

## 📝 Key Files Modified

1. **Dockerfile** - JDK 21 upgrade, Scala 3.4.2 fix
2. **nginx.conf** - HTTPS configuration
3. **docker-compose.yml** - Port updates, certificates, environment vars
4. **scripts/generate-ssl-certificates.sh** - Certificate generation
5. **HTTPS_QUICKSTART.md** - HTTPS documentation

---

## ✨ Next Steps

1. ✅ **Start services using manual script** → `bash start-https.sh`
2. ✅ **Test HTTPS access** → `curl -k https://localhost:9443/health`
3. ✅ **Configure frontend apps** → Update Nuxt configs with `https://localhost:9443`
4. ✅ **For production** → Use Let's Encrypt certificates

