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

- JDK 11+
- sbt 1.9.8
- PostgreSQL 14+

### Database Setup

```bash
# Create database
createdb consultant

# Run migrations (manual for now)
psql -d consultant -f data/src/main/resources/db/migration/V001__initial_schema.sql
```

### Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
# Edit .env with your settings
```

### Running

```bash
# Compile all projects
sbt compile

# Run the API server
sbt api/run

# With auto-reload
sbt api/reRun
```

Server will start on `http://localhost:8080`

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
