# Database Quick Reference

## 📊 Current Status

✅ **Docker PostgreSQL**: `consultant-db-master` (PostgreSQL 16.11)  
✅ **Local PostgreSQL**: System service (optional)

Both are configured with **same credentials**:
- **User**: `consultant_user`
- **Password**: `consultant_pass`
- **Database**: `consultant`
- **Port**: `5432`

---

## 🚀 Quick Commands

### Check Database Status
```bash
./scripts/db-manager.sh status
```

### Switch to Docker
```bash
./scripts/db-manager.sh docker
```

### Switch to Local
```bash
./scripts/db-manager.sh local
```

### Backup Database
```bash
./scripts/db-manager.sh backup
# Creates: backups/consultant_backup_YYYYMMDD_HHMMSS.sql
```

### Run Migrations
```bash
./scripts/db-manager.sh migrate
```

---

## 💻 Direct Database Access

### Docker
```bash
# Via psql
psql postgresql://consultant_user:consultant_pass@localhost:5432/consultant

# Via Docker
docker exec -it consultant-db-master psql -U consultant_user -d consultant
```

### Local
```bash
# Via psql
psql postgresql://consultant_user:consultant_pass@localhost:5432/consultant

# Via sudo
sudo -u postgres psql
```

---

## 🔄 Recommended Workflow

### For Development
1. **Use Docker** (no local PostgreSQL needed)
   ```bash
   ./scripts/db-manager.sh docker
   bash start-https.sh
   ```

2. **Use Local** (if you prefer local database)
   ```bash
   ./scripts/db-manager.sh local
   sbt api/run
   ```

### For Production
- Keep Docker option as backup
- Use managed database (RDS, etc.) in production
- Update `.env` with production credentials

---

## 📁 Related Files

| File | Purpose |
|------|---------|
| [DATABASE_CONFIG.md](DATABASE_CONFIG.md) | Detailed setup guide |
| [scripts/db-manager.sh](scripts/db-manager.sh) | Database management script |
| [start-https.sh](start-https.sh) | Full HTTPS stack (includes Docker DB) |
| [docker-compose.yml](docker-compose.yml) | Docker Compose configuration |

---

## 🛠️ Managing Docker Database

### Start
```bash
docker start consultant-db-master
```

### Stop
```bash
docker stop consultant-db-master
```

### View Logs
```bash
docker logs consultant-db-master
```

### Remove Container
```bash
docker rm consultant-db-master
# Volume preserved: consultant-db-data
```

### Remove Everything
```bash
docker rm consultant-db-master
docker volume rm consultant-db-data
```

---

## 📝 Environment Variables

In your `.env`:
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
```

No changes needed when switching between Docker and Local (same credentials!)

---

## ✨ Key Features

✅ **Zero-configuration switching** - Same credentials for both  
✅ **Automated backups** - One command backup  
✅ **Health checks** - Verify database availability  
✅ **Easy cleanup** - Complete removal without orphaned data  
✅ **Cross-platform** - Works on Linux, Mac, Windows  

---

## 🐛 Troubleshooting

### "Connection refused"
```bash
./scripts/db-manager.sh status
# Check which database is actually running
```

### "Port 5432 already in use"
```bash
# Kill the process
fuser -k 5432/tcp

# Or use different port
# Edit docker-compose.yml or postgresql.conf
```

### "Database consultant does not exist"
```bash
# Create it (Docker)
docker exec consultant-db-master psql -U consultant_user -c "CREATE DATABASE consultant WITH OWNER consultant_user;"

# Or create it (Local)
sudo -u postgres createdb -O consultant_user consultant
```

### "Authentication failed for user"
```bash
# Reset password (Docker)
docker exec consultant-db-master psql -U consultant_user -d consultant

# Reset password (Local)  
sudo -u postgres psql
ALTER USER consultant_user WITH PASSWORD 'consultant_pass';
```

---

## 📚 More Information

- [DATABASE_CONFIG.md](DATABASE_CONFIG.md) - Full configuration guide
- [README.md](README.md) - Project overview with HTTPS section
- [HTTPS_QUICKSTART.md](HTTPS_QUICKSTART.md) - HTTPS setup guide  
- [HTTPS_SETUP_STATUS.md](HTTPS_SETUP_STATUS.md) - HTTPS implementation details

