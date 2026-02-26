# Database Configuration - Docker vs Local

This project supports both Docker and local PostgreSQL installations.

## Current Status

| Type | Status | Details |
|------|--------|---------|
| **Docker PostgreSQL** | ✅ Running | `consultant-db-master` (PostgreSQL 16.11) |
| **Local PostgreSQL** | ❓ Optional | Can be installed and configured |

---

## Option 1: Using Docker PostgreSQL (Recommended - Current Setup)

### Status
✅ **Already running and configured**

### Connection Details
```
Host:     localhost
Port:     5432
Database: consultant
User:     consultant_user
Password: consultant_pass
```

### Access

**From command line:**
```bash
psql postgresql://consultant_user:consultant_pass@localhost:5432/consultant
```

**From Docker:**
```bash
docker exec -it consultant-db-master psql -U consultant_user -d consultant
```

### Configuration in .env
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
DB_DIALECT=postgres
```

### Start/Stop Docker Database

**Start:**
```bash
docker run -d \
  --name consultant-db-master \
  -e POSTGRES_DB=consultant \
  -e POSTGRES_USER=consultant_user \
  -e POSTGRES_PASSWORD=consultant_pass \
  -p 5432:5432 \
  postgres:16-alpine
```

**Stop:**
```bash
docker stop consultant-db-master
```

**Remove:**
```bash
docker rm consultant-db-master
```

---

## Option 2: Using Local PostgreSQL Installation

### Installation

**1. Install PostgreSQL:**
```bash
sudo apt-get update
sudo apt-get install -y postgresql postgresql-contrib libpq-dev
```

**2. Start PostgreSQL Service:**
```bash
sudo systemctl restart postgresql
sudo systemctl enable postgresql
```

**3. Create Database and User:**
```bash
sudo -u postgres psql <<EOF
CREATE USER consultant_user WITH PASSWORD 'consultant_pass';
CREATE DATABASE consultant WITH OWNER consultant_user;
GRANT ALL PRIVILEGES ON DATABASE consultant TO consultant_user;
ALTER USER consultant_user CREATEDB;
EOF
```

**4. Verify:**
```bash
psql postgresql://consultant_user:consultant_pass@localhost:5432/consultant
```

### Configuration in .env
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
DB_DIALECT=postgres
```

### Start/Stop Local Database

**Start:**
```bash
sudo systemctl start postgresql
```

**Stop:**
```bash
sudo systemctl stop postgresql
```

**Status:**
```bash
sudo systemctl status postgresql
```

---

## Switching Between Docker and Local

### From Docker to Local

**Step 1: Stop Docker**
```bash
docker stop consultant-db-master
```

**Step 2: Install and start local PostgreSQL**
```bash
# Follow "Installation" section above
```

**Step 3: Update .env if needed**
```bash
# Use same credentials, should work as-is
```

### From Local to Docker

**Step 1: Stop local PostgreSQL**
```bash
sudo systemctl stop postgresql
```

**Step 2: Start Docker**
```bash
docker start consultant-db-master
# Or use bash start-https.sh to start entire stack
```

---

## Database Migrations

Migrations are in `data/src/main/resources/db/migration/`

Run migrations with your backend startup:
```bash
sbt api/run
```

---

## Backup & Restore

### Backup (Docker)
```bash
docker exec consultant-db-master pg_dump -U consultant_user -d consultant > backup.sql
```

### Backup (Local)
```bash
pg_dump -U consultant_user -d consultant > backup.sql
```

### Restore
```bash
# Docker
docker exec -i consultant-db-master psql -U consultant_user -d consultant < backup.sql

# Local
psql -U consultant_user -d consultant < backup.sql
```

---

## Troubleshooting

### Docker: Connection refused
```bash
# Check if container is running
docker ps | grep consultant-db

# Check logs
docker logs consultant-db-master

# Restart
docker restart consultant-db-master
```

### Local: Socket not found
```bash
# Check service status
sudo systemctl status postgresql

# Restart
sudo systemctl restart postgresql

# Check logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

### Port 5432 already in use
```bash
# Find process using port
lsof -i :5432

# Kill process
kill -9 <PID>

# Or use different port in docker-compose.yml
```

---

## Performance Tips

### Docker PostgreSQL
- Allocate enough memory: `-m 1g`
- Use volume mounts for persistence
- Tune PostgreSQL parameters

### Local PostgreSQL
- Check memory: `free -h`
- Monitor with: `top -p $(pgrep postgres)`
- Adjust `shared_buffers`, `work_mem` in postgresql.conf

---

## Testing Database Connection

### Using psql
```bash
psql postgresql://consultant_user:consultant_pass@localhost:5432/consultant -c "SELECT 1;"
```

### Using Scala/Doobie (from backend)
```bash
sbt api/run
# Backend will connect on startup
```

### Using curl to test API
```bash
curl -k https://localhost:9443/health
```

