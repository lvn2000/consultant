# System Architecture

The Consultant Backend uses **Hexagonal Architecture** (Ports & Adapters) to ensure easy migration to AWS:

```
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

## Architecture Principles

### Hexagonal Architecture Benefits

1. **Separation of Concerns**: Clear boundaries between business logic, data access, and external services
2. **Testability**: Easy to unit test business logic without external dependencies
3. **Flexibility**: Can swap implementations without changing core logic
4. **AWS Readiness**: Easy migration to AWS services using the adapter pattern

### Domain Model Overview

- **Users (clients)**: Regular users who book consultations
- **Specialists**: Experts who provide consultations with categories, ratings, and availability
- **Consultations**: Track consultation requests, status, and outcomes
- **Categories**: Hierarchical classification system for specialties
- **Connections**: Communication channels (WhatsApp, Viber, etc.)

### Ports & Adapters Pattern

The system uses the Ports & Adapters pattern where:

- **Ports** are interfaces defined in the core layer
- **Adapters** implement these interfaces in the data and infrastructure layers
- This allows for easy swapping of implementations (e.g., PostgreSQL to AWS DynamoDB)

## Technology Stack

- **Scala 3.3.1** - Main programming language
- **Cats & Cats Effect** - Functional programming paradigm
- **Http4s** - HTTP server implementation
- **Tapir** - Type-safe API definitions
- **Doobie** - Functional database access
- **Circe** - JSON serialization/deserialization
- **Ciris** - Configuration management
- **AWS SDK 2** - Cloud services integration
- **FS2-AWS** - Reactive AWS streaming

## Scalability Considerations

The architecture is designed with scalability in mind:

- **Stateless API Layer**: Can be scaled horizontally behind a load balancer
- **Database Optimization**: Proper indexing and query optimization in PostgreSQL
- **AWS Integration**: Ready for S3, SES, SNS, and SQS for distributed processing
- **Event-Driven Architecture**: Asynchronous processing with message queues

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