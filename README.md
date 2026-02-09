# Consultant Backend - Multi-Project Structure

Scala-based backend system for connecting clients with specialists.

## Architecture

This project uses **Hexagonal Architecture** (Ports & Adapters) to ensure easy migration to AWS:

```text
┌─────────────────────────────────────────────────────────┐
│                        API Layer                        │
│  (HTTP endpoints, DTOs, Tapir routes)                  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                     Core Layer                          │
│  (Domain models, Business logic, Port interfaces)      │
└─────────────┬──────────────────────────┬────────────────┘
              │                          │
┌─────────────▼──────────┐   ┌──────────▼──────────────┐
│    Data Layer          │   │  Infrastructure Layer   │
│  (PostgreSQL repos)    │   │  (AWS/Local adapters)   │
└────────────────────────┘   └─────────────────────────┘
```

## Project Structure

### **core** - Domain & Business Logic

- `domain/` - Domain models (User, Specialist, Consultation, Category)
- `service/` - Business services
- `ports/` - Interface definitions for infrastructure

### **data** - PostgreSQL Implementation

- `repository/` - Doobie-based PostgreSQL repositories
- `config/` - Database configuration
- `resources/db/migration/` - SQL migrations

### **infrastructure** - External Service Adapters

- `aws/` - AWS implementations (S3, SES, SNS, SQS)
- `local/` - Local/mock implementations for development
- `config/` - Application configuration (Ciris)

### **api** - HTTP API

- `routes/` - Tapir endpoint definitions
- `dto/` - Data Transfer Objects
- `Server.scala` - Main application

## Features

✅ **Domain Models:**

- Users (clients)
- Specialists with categories, ratings, availability
- Consultations with status tracking
- Hierarchical categories

✅ **Search & Matching:**

- Search specialists by category, rating, price, experience
- Consultation request handling
- Rating and review system

✅ **AWS-Ready:**

- S3 for file storage
- SES for emails, SNS for SMS
- SQS for async messaging
- Easy toggle between local and AWS implementations

## Getting Started

### Prerequisites

- Docker & Docker Compose
- JDK 21+ (for local development)
- sbt 1.9.8 (for local development)

### Quick Start (Recommended for Development)

```bash
# 1. Navigate to backend directory
cd backend

# 2. Start the API (PostgreSQL starts automatically in Docker)
./run.sh
```

The script will:
- Load environment variables from `.env`
- Start PostgreSQL 16 in Docker
- Run all Flyway database migrations automatically (24 migrations)
- Start the API on `http://localhost:8090`

✅ **That's it!** Database is fully initialized and ready.

### Configuration

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

### Database Initialization

**Automatic with Flyway:**
- All 24 migrations run automatically on API startup
- Located in: `data/src/main/resources/db/migration/V*.sql`
- Each migration runs only once (tracked in `flyway_schema_history` table)
- Includes schema, test data, and constraints

No manual database setup required! ✅

### Running

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

### For a Developer Seeing This Project for the First Time

#### Prerequisites Check
```bash
docker --version     # Docker 20.10+
java -version        # JDK 21+
sbt --version        # sbt 1.9.8+
```

#### Step 1: Clone & Navigate
```bash
git clone https://github.com/lvn2000/consultant.git
cd consultant/backend
```

#### Step 2: Start the API
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
- ✅ Runs all 24 Flyway migrations
- ✅ Seeds test data
- ✅ Starts API server

#### Step 3: Verify Everything Works

In a **new terminal**:
```bash
# Test API health
curl http://localhost:8090/api/health

# View Swagger documentation
open http://localhost:8090/docs
```

#### Step 4: Test Login

```bash
curl -X POST http://localhost:8090/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"login":"user","password":"user"}'
```

**Test Users Available:**

| Login | Password | Role | Email |
|-------|----------|------|-------|
| `user` | `user` | Client | user@example.com |
| `admin` | `admin` | Admin | admin@admin.com |
| `spec` | `spec` | Specialist | spec@example.com |

#### Step 5: Setup Frontend Apps (Optional)

In **separate terminals**:

```bash
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

#### Access Your Applications

| Application | URL | Login | Password |
|-------------|-----|-------|----------|
| **API Documentation** | `http://localhost:8090/docs` | N/A | N/A |
| **Client App** | `http://localhost:3000` | user | user |
| **Admin App** | `http://localhost:3001` | admin | admin |
| **Specialist App** | `http://localhost:3002` | spec | spec |

#### Common Commands

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

#### Troubleshooting

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

#### Project Structure

```
backend/
├── api/              # HTTP routes & endpoints
├── core/             # Business logic & services
├── data/             # PostgreSQL & Flyway migrations
├── infrastructure/   # AWS/Local adapters
├── .env              # Configuration (loaded by run.sh)
├── run.sh            # ⭐ Use this to start
└── start-https.sh    # Production HTTPS deployment

client-app/          # Nuxt.js frontend for customers
admin-app/           # Nuxt.js frontend for admins
specialist-app/      # Nuxt.js frontend for specialists
```

#### That's It! 🎉

You now have:
- ✅ Running API on `http://localhost:8090`
- ✅ PostgreSQL database with all migrations applied
- ✅ Test data ready to use
- ✅ Swagger documentation available

Start exploring the codebase or building features!

## HTTPS Setup with Docker

This project supports HTTPS with automatic HTTP→HTTPS redirect and security headers.

### Quick Start (Recommended)

```bash
# Start entire stack with HTTPS enabled
bash start-https.sh
```

This will:
- Build Docker image with JDK 21
- Start PostgreSQL, Redis, 3 API instances, and Nginx
- Enable HTTPS on port 9443
- Automatically redirect HTTP (9080) to HTTPS (9443)

### Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| **HTTPS** | `https://localhost:9443` | Secure backend access |
| **HTTP** | `http://localhost:9080` | Redirects to HTTPS automatically |
| **App-1** | `http://localhost:8081/health` | Direct instance access |
| **App-2** | `http://localhost:8082/health` | Direct instance access |
| **App-3** | `http://localhost:8083/health` | Direct instance access |

### Frontend Configuration

Update all Nuxt frontend apps (`client-app`, `admin-app`, `specialist-app`) with:

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      apiBase: 'https://localhost:9443',  // Use HTTPS!
    }
  }
})
```

Or via environment variable:
```bash
NUXT_PUBLIC_API_BASE=https://localhost:9443
```

### HTTPS Features Enabled

✅ TLS 1.2 & 1.3  
✅ HTTP/2 Support  
✅ HSTS Header (1-year)  
✅ Secure Cookies  
✅ Self-signed certificates (development)  
✅ Load balancing across 3 app instances  

### Certificate Management

**Development (Self-signed):**
```bash
# Certificates already generated at ./certs/
# Valid for 365 days
```

**Production (Let's Encrypt):**
```bash
# Generate Let's Encrypt certificate
bash scripts/generate-ssl-certificates.sh
# Select 'p' for production
```

See [HTTPS_QUICKSTART.md](HTTPS_QUICKSTART.md) for detailed setup instructions.

### Docker Compose Alternative

If you prefer docker-compose:
```bash
docker-compose up -d
```

⚠️ Note: Some docker-compose versions may have compatibility issues. Use `bash start-https.sh` if you encounter errors.

### Testing HTTPS

```bash
# Test HTTPS endpoint
curl -k https://localhost:9443/health

# Test HTTP redirect
curl -i http://localhost:9080/

# View security headers
curl -k https://localhost:9443 -I | grep Strict-Transport-Security
```

## Initial Test Data

The system includes default test credentials for development and testing purposes. These are created automatically during the first database migration.

### ⚠️ CRITICAL - SECURITY WARNING

**You MUST change these credentials immediately after deploying to production!**

### Test Accounts

| Account | Email | Password | Role | Phone |
|---------|-------|----------|------|-------|
| **Admin** | admin@admin.com | admin | Admin | N/A |
| **User** | user@example.com | user | Client | +1234567890 |
| **Specialist** | spec@example.com | spec | Specialist | +9876543210 |

### Test Specialist Details

- **Hourly Rate:** $50.00
- **Experience:** 5 years
- **Status:** Available
- **Connection:** WhatsApp (+9876543210)

### Default UUIDs for Testing

```
Test User ID:       11111111-1111-1111-1111-111111111111
Test Specialist ID: 22222222-2222-2222-2222-222222222222
```

### How to Remove Test Data

For production or when ready to remove test accounts:

```sql
DELETE FROM specialist_connections WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialist_categories WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialists WHERE id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM credentials WHERE email IN ('user@example.com', 'spec@example.com');
DELETE FROM users WHERE id IN ('11111111-1111-1111-1111-111111111111'::uuid, '22222222-2222-2222-2222-222222222222'::uuid);
```

For more details, see [TEST_CREDENTIALS.md](TEST_CREDENTIALS.md)

## API Endpoints

### Users

- `POST /api/users` - Create user
- `GET /api/users/:id` - Get user
- `GET /api/users?offset=0&limit=20` - List users

### Specialists

- `POST /api/specialists` - Register specialist
- `GET /api/specialists/:id` - Get specialist
- `GET /api/specialists/search?categoryId=...&minRating=...` - Search specialists

### Consultations

- `POST /api/consultations` - Create consultation request
- `GET /api/consultations/:id` - Get consultation
- `GET /api/consultations/user/:userId` - User's consultations
- `GET /api/consultations/specialist/:specialistId` - Specialist's consultations
- `POST /api/consultations/:id/review` - Add review

### Categories

- `POST /api/categories` - Create category
- `GET /api/categories/:id` - Get category
- `GET /api/categories` - List all categories

### Connections

- `GET /api/connection-types` - List available connection types (Viber, WhatsApp, Slack, etc.)
- `GET /api/connection-types/:id` - Get specific connection type
- `POST /api/specialists/:specialistId/connections` - Add connection for specialist
- `GET /api/specialists/:specialistId/connections` - Get specialist's connections
- `GET /api/specialists/:specialistId/connections/:connectionId` - Get specific connection
- `PUT /api/specialists/:specialistId/connections/:connectionId` - Update connection
- `DELETE /api/specialists/:specialistId/connections/:connectionId` - Remove connection

### Documentation

- `GET /docs` - Swagger UI

## Migration to AWS

To migrate to AWS, simply change the configuration:

```bash
USE_AWS=true
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket
AWS_SQS_QUEUE_PREFIX=consultant
AWS_SENDER_EMAIL=[email protected]
```

The infrastructure layer will automatically use AWS services instead of local implementations.

## Technology Stack

- **Scala 3.3.1**
- **Cats & Cats Effect** - Functional programming
- **Http4s** - HTTP server
- **Tapir** - Type-safe API definitions
- **Doobie** - PostgreSQL access
- **Circe** - JSON handling
- **Ciris** - Configuration management
- **AWS SDK 2** - AWS services integration
- **FS2-AWS** - Reactive AWS streaming

## License

TBD
