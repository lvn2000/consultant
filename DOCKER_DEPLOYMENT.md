# Docker Deployment Guide

Complete guide for running the Consultant application in Docker (locally and on Dokploy).

## 📋 Project Structure

```
backend/
├── Dockerfile                    # Backend (Scala/Http4s)
├── frontend.Dockerfile           # Frontend apps (Nuxt.js) - shared for all 3 apps
├── docker-compose.app.yml        # Base configuration
├── docker-compose.dev.yml        # Development overrides
├── docker-compose.prod.yml       # Production overrides (Dokploy)
├── .dockerignore                 # Docker build exclusions
├── nginx.conf                    # Nginx configuration (optional)
│
├── admin-app/                    # Admin panel (Nuxt.js)
│   ├── package.json
│   └── ...
│
├── client-app/                   # Client app (Nuxt.js)
│   ├── package.json
│   └── ...
│
└── specialist-app/               # Specialist app (Nuxt.js)
    ├── package.json
    └── ...
```

## 🚀 Quick Start

### Local Development

**Option 1: Without Docker (Current Workflow)**

Continue developing as you do now - Docker is only for deployment:

```bash
# Terminal 1 - Backend
./run.sh

# Terminal 2 - Admin App
cd admin-app && npm run dev

# Terminal 3 - Client App
cd client-app && npm run dev

# Terminal 4 - Specialist App
cd specialist-app && npm run dev
```

**Option 2: In Docker (For Testing)**

```bash
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml up
```

**Access URLs:**
- Backend API: http://localhost:8090
- Health Check: http://localhost:8090/health
- Admin App: http://localhost:3000
- Client App: http://localhost:3001
- Specialist App: http://localhost:3002

> **Note:** SwaggerUI is temporarily disabled in the Docker build due to webjars resource merging issues in sbt-assembly. To re-enable, uncomment the code in `api/src/main/scala/com/consultant/api/Server.scala` and update the merge strategy in `build.sbt`.

### Production (Dokploy)

```bash
docker compose -f docker-compose.app.yml -f docker-compose.prod.yml up -d
```

## 🎯 Dokploy Deployment

### Step 1: Prepare Project

1. Create a new project in Dokploy
2. Select **"Docker Compose"** as the deployment type
3. Upload the following files to your repository:
   - `docker-compose.app.yml`
   - `docker-compose.prod.yml`
   - `Dockerfile` (backend)
   - `frontend.Dockerfile` (frontend apps)
   - `nginx.conf` (optional, for reverse proxy)

### Step 2: Environment Variables

Create a `.env` file or configure in Dokploy Secrets:

```env
# Database
POSTGRES_DB=consultant
POSTGRES_USER=consultant_user
POSTGRES_PASSWORD=<secure-password>

# Security - Generate strong random values!
DB_ENCRYPTION_KEY=<generate-32-character-random-key>
JWT_SECRET=<generate-64-character-random-secret>
SESSION_SECRET=<generate-32-character-random-secret>

# Server
SERVER_HOST=0.0.0.0
SERVER_PORT=8090

# CORS (your production domain)
CORS_ORIGINS=https://your-domain.com

# JWT Configuration
JWT_ISSUER=consultant-api
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d

# Security Settings
MAX_LOGIN_ATTEMPTS=5
ACCOUNT_LOCK_DURATION_MINUTES=15
FORCE_HTTPS=true
SECURE_COOKIES=true
SESSION_SECURE=true
```

### Step 3: Deploy

Dokploy will automatically:
1. Build all Docker images
2. Start containers
3. Configure networking between services
4. Set up health checks

## 📦 Docker Images

Each application runs in a separate container:

| Image | Description | Port | Build Context |
|-------|-------------|------|---------------|
| `backend-backend` | Backend API (Scala/Http4s) | 8090 | `.` |
| `backend-admin-app` | Admin Panel (Nuxt.js) | 3000 | `.` (with APP_DIR=admin-app) |
| `backend-client-app` | Client App (Nuxt.js) | 3001 | `.` (with APP_DIR=client-app) |
| `backend-specialist-app` | Specialist App (Nuxt.js) | 3002 | `.` (with APP_DIR=specialist-app) |
| `postgres:16-alpine` | PostgreSQL Database | 5432 | External |

## 🔧 Docker Commands

### Build Images

```bash
# Backend only
docker build -t consultant-backend -f Dockerfile .

# Frontend apps (using shared Dockerfile)
docker build --build-arg APP_DIR=admin-app -t consultant-admin -f frontend.Dockerfile .
docker build --build-arg APP_DIR=client-app -t consultant-client -f frontend.Dockerfile .
docker build --build-arg APP_DIR=specialist-app -t consultant-specialist -f frontend.Dockerfile .

# Or use docker compose
docker compose -f docker-compose.app.yml build
```

### Start Services

```bash
# Development mode (no nginx)
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml up

# Production mode with nginx (detached)
docker compose -f docker-compose.app.yml -f docker-compose.prod.yml --profile production up -d

# Production mode without nginx
docker compose -f docker-compose.app.yml -f docker-compose.prod.yml up -d

# Specific service only
docker compose -f docker-compose.app.yml up -d backend
```

> **Note:** Nginx is disabled by default in development mode. It only starts when using the `--profile production` flag. This prevents port 80 conflicts during local development.

### Stop Services

```bash
# Stop all services
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml down

# Stop and remove volumes (WARNING: deletes database!)
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml down -v

# Stop specific service
docker compose -f docker-compose.app.yml stop backend
```

### View Logs

```bash
# All services
docker compose -f docker-compose.app.yml logs -f

# Specific service
docker compose -f docker-compose.app.yml logs -f backend
docker compose -f docker-compose.app.yml logs -f admin-app
docker compose -f docker-compose.app.yml logs -f postgres

# Last 100 lines
docker compose -f docker-compose.app.yml logs --tail=100 backend
```

### Rebuild

```bash
# With cache
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml up --build

# Without cache (full rebuild)
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml up --build --no-cache

# Rebuild specific service
docker compose -f docker-compose.app.yml build --no-cache backend
```

### Service Status

```bash
# Check running containers
docker compose -f docker-compose.app.yml ps

# Detailed info
docker inspect consultant-backend
```

## 🗂️ Nginx Configuration (Optional)

### Development vs Production

**Development:** Nginx is **disabled by default** to avoid port conflicts (port 80 may be in use by other services). Access apps directly:
- http://localhost:8090 (Backend)
- http://localhost:3000 (Admin)
- http://localhost:3001 (Client)
- http://localhost:3002 (Specialist)

**Production (Dokploy):** Enable nginx for unified routing and SSL termination:

```bash
docker compose -f docker-compose.app.yml -f docker-compose.prod.yml --profile production up -d
```

### Nginx Routing

For production, Nginx routes traffic to different services:

```nginx
location /api/ {
    proxy_pass http://backend:8090;
}

location /admin/ {
    proxy_pass http://admin-app:3000;
}

location /client/ {
    proxy_pass http://client-app:3000;
}

location /specialist/ {
    proxy_pass http://specialist-app:3000;
}
```

With this configuration, all services are accessible through a single domain:
- `https://your-domain.com/api/` → Backend
- `https://your-domain.com/admin/` → Admin App
- `https://your-domain.com/client/` → Client App
- `https://your-domain.com/specialist/` → Specialist App

## 🔒 Security Checklist

### Before Production Deployment:

- [ ] Change all default passwords (PostgreSQL, etc.)
- [ ] Generate new `JWT_SECRET` (64+ characters)
- [ ] Generate new `SESSION_SECRET` (32+ characters)
- [ ] Generate new `DB_ENCRYPTION_KEY` (32 characters)
- [ ] Enable HTTPS with SSL certificates
- [ ] Configure CORS for your production domain only
- [ ] Restrict PostgreSQL access (no public exposure)
- [ ] Use Dokploy Secrets for sensitive variables
- [ ] Enable `FORCE_HTTPS=true` in production
- [ ] Enable `SECURE_COOKIES=true` in production
- [ ] Set up regular backups for PostgreSQL volume

## ⚠️ Troubleshooting

### Port Already in Use

```bash
# Find process using the port
lsof -ti:8090

# Kill the process
lsof -ti:8090 | xargs kill -9

# Or use a different port in docker-compose.dev.yml
```

### Frontend Build Errors

```bash
# Clear cache and rebuild
cd admin-app
rm -rf node_modules .nuxt .output
npm install

# Rebuild Docker image
docker compose -f docker-compose.app.yml build --no-cache admin-app
```

### Backend Cannot Connect to PostgreSQL

1. Check services are on the same network:
```bash
docker network inspect backend_consultant-network
```

2. Verify database is healthy:
```bash
docker compose -f docker-compose.app.yml ps postgres
```

3. Check backend logs:
```bash
docker compose -f docker-compose.app.yml logs backend
```

4. Ensure `DB_HOST` is set to `postgres` (Docker service name), not `localhost`

### Container Exits Immediately

```bash
# Check logs for errors
docker compose -f docker-compose.app.yml logs backend

# Run interactively for debugging
docker run --rm -it backend-backend sh
```

### Database Migration Issues

If Flyway fails:

```bash
# Reset database (WARNING: deletes all data!)
docker compose -f docker-compose.app.yml down -v
docker compose -f docker-compose.app.yml up -d postgres

# Or set baselineOnMigrate=true in Server.scala (already configured)
```

## 📊 Resource Allocation

### Development

| Service | CPU | Memory |
|---------|-----|--------|
| Backend | 1.0 | 1GB |
| Each Frontend | 0.5 | 256MB |
| PostgreSQL | 1.0 | 512MB |

### Production

| Service | CPU | Memory |
|---------|-----|--------|
| Backend | 2.0 | 2GB |
| Each Frontend | 1.0 | 512MB |
| PostgreSQL | 2.0 | 2GB |

Configure in `docker-compose.prod.yml` under `deploy.resources`.

## 🔄 Development Workflow

### Typical Local Development Session

```bash
# 1. Start database only (if you run backend locally)
docker compose -f docker-compose.app.yml up -d postgres

# 2. Run backend locally
./run.sh

# 3. Run frontend apps in separate terminals
cd admin-app && npm run dev
cd client-app && npm run dev
cd specialist-app && npm run dev
```

### Testing Docker Build

```bash
# Build and test locally before deploying
docker compose -f docker-compose.app.yml -f docker-compose.dev.yml up --build

# Verify all services are healthy
docker compose -f docker-compose.app.yml ps

# Check backend health endpoint
curl http://localhost:8090/health
```

## 📝 Configuration Reference

### Backend Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_HOST` | Server bind host | `0.0.0.0` |
| `SERVER_PORT` | Server port | `8090` |
| `DB_HOST` | PostgreSQL host | `postgres` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `consultant` |
| `DB_USER` | Database user | `consultant_user` |
| `DB_PASSWORD` | Database password | - |
| `DB_ENCRYPTION_KEY` | Encryption key for sensitive data | - |
| `JWT_SECRET` | JWT signing secret | - |
| `JWT_ISSUER` | JWT issuer claim | `consultant-api` |
| `JWT_ACCESS_TTL` | Access token TTL | `15m` |
| `JWT_REFRESH_TTL` | Refresh token TTL | `7d` |
| `SESSION_SECRET` | Session encryption secret | - |
| `CORS_ORIGINS` | Comma-separated allowed origins | - |
| `FORCE_HTTPS` | Require HTTPS | `false` |
| `MAX_LOGIN_ATTEMPTS` | Max failed login attempts | `5` |
| `ACCOUNT_LOCK_DURATION_MINUTES` | Account lock duration | `15` |

### Frontend Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NODE_ENV` | Node environment | `production` |
| `NUXT_PUBLIC_API_BASE` | Backend API URL | `http://backend:8090/api` |
| `NUXT_HOST` | Server bind host | `0.0.0.0` |
| `NUXT_PORT` | Server port | `3000` |

## 🆘 Getting Help

1. Check logs: `docker compose -f docker-compose.app.yml logs <service>`
2. Verify health: `docker compose -f docker-compose.app.yml ps`
3. Test connectivity: `docker exec -it consultant-backend curl http://postgres:5432`
4. Review configuration files for typos
5. Check Docker daemon: `docker info`

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Dokploy Documentation](https://dokploy.com/docs)
- [Nuxt.js Deployment Guide](https://nuxt.com/docs/getting-started/deployment)
- [Http4s Server Documentation](https://http4s.org/)
