# Database Quick Reference

## 📊 Current Status

✅ **Docker PostgreSQL**: Running via `start-https.sh` or `docker-compose up`
✅ **Database**: PostgreSQL 16+ with Flyway migrations

**Current credentials** (from `.env.example`):
- **User**: `postgres`
- **Password**: `postgres`
- **Database**: `consultant`
- **Port**: `5432`

---

## 🚀 Quick Start

### Start Database and API

```bash
# Recommended: Start entire stack with HTTPS
bash start-https.sh

# Or use docker-compose directly
docker-compose up -d

# Or start API only (PostgreSQL must be running separately)
cd /home/lvn/prg/scala/Consultant/backend
./run.sh
```

### Access Database

```bash
# Connect via psql
PGPASSWORD=postgres psql -h localhost -U postgres -d consultant

# Or via Docker (if using docker-compose)
docker exec -it <container-name> psql -U postgres -d consultant
```

---

## 💻 Direct Database Access

### Connection String
```
postgresql://postgres:postgres@localhost:5432/consultant
```

### Using psql
```bash
# With password in command
PGPASSWORD=postgres psql -h localhost -U postgres -d consultant

# Or interactively
psql -h localhost -U postgres -d consultant
# Then enter password when prompted
```

---

## 🔄 Recommended Workflow

### For Development

1. **Using Docker (Recommended)**:
   ```bash
   # Start entire stack
   bash start-https.sh
   
   # API will be available at http://localhost:8090
   # Database migrations run automatically
   ```

2. **Using Local PostgreSQL**:
   ```bash
   # Ensure PostgreSQL is running
   sudo systemctl start postgresql
   
   # Create database and user
   sudo -u postgres psql -c "CREATE DATABASE consultant;"
   sudo -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'postgres';"
   sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE consultant TO postgres;"
   
   # Run API
   ./run.sh
   ```

### For Production

- Use managed database services (AWS RDS, Google Cloud SQL, etc.)
- Update `.env` with production credentials
- Never use development credentials in production

---

## 📁 Related Files

| File | Purpose |
|------|---------|
| [DATABASE_CONFIG.md](DATABASE_CONFIG.md) | Detailed setup guide |
| [start-https.sh](start-https.sh) | Full HTTPS stack startup script |
| [docker-compose.yml](docker-compose.yml) | Docker Compose configuration |
| [.env.example](.env.example) | Environment variables template |

---

## 🛠️ Managing Docker Database

### View Running Containers
```bash
docker ps | grep postgres
```

### Start Container
```bash
docker start <container-name>
```

### Stop Container
```bash
docker stop <container-name>
```

### View Logs
```bash
docker logs <container-name>
```

### Remove Container
```bash
docker rm -f <container-name>
```

### Remove Volume (deletes all data!)
```bash
docker volume rm <volume-name>
```

---

## 📝 Environment Variables

In your `.env` file:

```bash
# Database configuration
DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://localhost:5432/consultant
DB_USER=postgres
DB_PASSWORD=postgres
DB_POOL_SIZE=32
```

---

## ✨ Key Features

✅ **Automatic migrations** - Flyway runs all migrations on startup
✅ **Test data included** - Default test users created automatically
✅ **Docker support** - Easy setup with docker-compose
✅ **Local support** - Works with local PostgreSQL installation
✅ **Health checks** - Database health monitored in Docker

---

## 🐛 Troubleshooting

### "Connection refused"
```bash
# Check if PostgreSQL is running
docker ps | grep postgres
# or
sudo systemctl status postgresql
```

### "Port 5432 already in use"
```bash
# Find process using port 5432
lsof -i :5432

# Kill the process
kill -9 <PID>
```

### "Database consultant does not exist"
```bash
# Create it
PGPASSWORD=postgres psql -h localhost -U postgres -c "CREATE DATABASE consultant;"
```

### "Authentication failed for user postgres"
```bash
# Check your .env file matches the actual password
# Default: postgres / postgres
```

### Migration errors
```bash
# Check migration status in database
PGPASSWORD=postgres psql -h localhost -U postgres -d consultant -c "SELECT * FROM flyway_schema_history;"

# To reset (DEVELOPMENT ONLY - deletes all data!)
docker-compose down -v
docker-compose up -d
```

---

## 📚 More Information

- [DATABASE_CONFIG.md](DATABASE_CONFIG.md) - Full configuration guide
- [README.md](README.md) - Project overview
- [HTTPS_QUICKSTART.md](HTTPS_QUICKSTART.md) - HTTPS setup guide
- [TEST_CREDENTIALS.md](TEST_CREDENTIALS.md) - Default test users
