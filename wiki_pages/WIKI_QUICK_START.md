# Quick Start Guide

This guide will help you get the Consultant Backend up and running quickly.

## Prerequisites

- Docker & Docker Compose
- JDK 21+ (for local development)
- sbt 1.9.8 (for local development)

## Quick Start (Recommended for Development)

```bash
# 1. Navigate to backend directory
cd backend

# 2. Start the API (PostgreSQL starts automatically in Docker)
./run.sh
```

The script will:
- Load environment variables from `.env`
- Start PostgreSQL 16 in Docker
- Run all Flyway database migrations automatically (2 migrations)
- Start the API on `http://localhost:8090`

✅ **That's it!** Database is fully initialized and ready.

## Configuration

Environment variables are loaded from `.env`. If you need custom settings:

```bash
# View current configuration
cat .env

# Key variables:
DB_URL=jdbc:postgresql://localhost:5432/consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
JWT_SECRET=your-secret-key
```

## Running the Application

```bash
# Start API server
./run.sh

# Or use sbt directly
sbt compile
sbt api/run

# With auto-reload on code changes
sbt api/reRun
```

API will start on `http://localhost:8090` with Swagger docs at `http://localhost:8090/docs`

## First-Time Developer Setup

### Prerequisites Check
```bash
docker --version     # Docker 20.10+
java -version        # JDK 21+
sbt --version        # sbt 1.9.8+
```

### Step 1: Clone & Navigate
```bash
git clone https://github.com/lvn2000/consultant.git
cd consultant/backend
```

### Step 2: Start the API
```bash
./run.sh
```

Wait for the message:
```
[info] Server listening on 0.0.0.0:8090
```

This single command automatically:
- ✅ Loads environment variables
- ✅ Starts PostgreSQL in Docker
- ✅ Runs all 2 Flyway migrations (V001, V002)
- ✅ Seeds test data
- ✅ Starts API server

### Step 3: Verify Everything Works

In a **new terminal**:
```bash
# Test API health
curl http://localhost:8090/api/health

# View Swagger documentation
open http://localhost:8090/docs
```

### Step 4: Test Login

```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"user","password":"user"}'
```

⚠️ **SECURITY WARNING**: The test credentials below are for **development and testing only**. Never use these credentials in production. Always change default passwords immediately after deploying to production!

**Test Users Available:**

| Login | Password | Role | Email |
|-------|----------|------|-------|
| `user` | `user` | Client | user@example.com |
| `admin` | `admin` | Admin | admin@admin.com |
| `spec` | `spec` | Specialist | spec@example.com |

🔒 **Production Security Checklist:**
- Change all default passwords before going live
- Use strong, unique passwords for each user
- Implement password complexity requirements
- Enable multi-factor authentication (if available)
- Regularly rotate credentials
- Monitor for unauthorized access attempts

### Step 5: Setup Frontend Apps (Optional)

In **separate terminals**:

```
# Terminal A: Client App
cd client-app && npm install && npm run dev
# Open http://localhost:3000

# Terminal B: Admin App
cd admin-app && npm install && npm run dev
# Open http://localhost:3001

# Terminal C: Specialist App
cd specialist-app && npm install && npm run dev
# Open http://localhost:3002
```

### Access Your Applications

| Application | URL | Login | Password |
|-------------|-----|-------|----------|
| **API Documentation** | `http://localhost:8090/docs` | N/A | N/A |
| **Client App** | `http://localhost:3000` | user | user |
| **Admin App** | `http://localhost:3001` | admin | admin |
| **Specialist App** | `http://localhost:3002` | spec | spec |

## Testing the API

Quick test scripts are available in `scripts/`:

```bash
# Test admin endpoints
./scripts/test-admin-count.sh

# Test specialist availability slots
./scripts/test-slots.sh

# Test specialists search
./scripts/test-specialists-rates.sh
./scripts/test-specialists-response.sh
```

## Common Commands

```bash
# Stop API
Ctrl+C

# Restart API
./run.sh

# Stop PostgreSQL container
docker stop consultant-db-master

# Query database directly
PGPASSWORD=consultant_pass psql -h localhost -U consultant_user -d consultant

# View all running containers
docker ps

# Clean up everything and start fresh
docker-compose down --volumes
./run.sh
```

## Troubleshooting

**Port 8090 already in use?**
```bash
lsof -i :8090
kill -9 <PID>
```

**Docker or database issues?**
```bash
# Remove containers and volumes
docker rm -f consultant-db-master
docker volume rm consultant-db-data 2>/dev/null || true

# Start fresh
./run.sh
```

**API won't start?**
```bash
# Check if PostgreSQL is running
docker ps | grep consultant-db

# View API logs (they print in terminal)
# Most errors will be visible in the ./run.sh terminal
```