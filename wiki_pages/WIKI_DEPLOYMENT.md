# Deployment Guide

This document provides comprehensive information for deploying the Consultant Backend application.

## Deployment Options

### Development Deployment

For local development and testing:

```bash
# Start the application with PostgreSQL
./run.sh

# The script will:
# - Load environment variables from .env
# - Start PostgreSQL in Docker
# - Run database migrations
# - Start the API server on http://localhost:8090
```

### HTTPS Development Deployment

For HTTPS-enabled development:

```bash
# Start the full HTTPS stack
bash start-https.sh

# This sets up:
# - 3 API instances behind Nginx load balancer
# - HTTPS on port 9443
# - HTTP to HTTPS redirect on port 9080
# - SSL certificate generation
```

### Production Deployment

For production environments, multiple options are available:

- Docker-based deployment
- Container orchestration (Kubernetes)
- Cloud platforms (AWS, Azure, GCP)
- Traditional server deployment

## Docker Deployment

### Prerequisites

- Docker Engine 20.10+
- Docker Compose v2+
- Sufficient system resources

### Docker Compose Deployment

```yaml
# docker-compose.yml
version: '3.8'

services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: consultant
      POSTGRES_USER: consultant_user
      POSTGRES_PASSWORD: consultant_pass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  api:
    build: .
    environment:
      - DB_URL=jdbc:postgresql://db:5432/consultant
      - DB_USER=consultant_user
      - DB_PASSWORD=consultant_pass
    depends_on:
      - db
    ports:
      - "8090:8090"

volumes:
  postgres_data:
```

### Building the Docker Image

```bash
# Build the application JAR
sbt assembly

# Build the Docker image
docker build -t consultant-backend .

# Or use the provided Dockerfile
docker build -f Dockerfile -t consultant-backend .
```

## Configuration

### Environment Variables

All configuration is handled through environment variables:

```bash
# Server configuration
SERVER_HOST=0.0.0.0
SERVER_PORT=8090

# Database configuration
DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://localhost:5432/consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
DB_POOL_SIZE=32

# AWS configuration
USE_AWS=false
AWS_REGION=us-east-1
AWS_S3_BUCKET=consultant-files
AWS_SQS_QUEUE_PREFIX=consultant
AWS_SENDER_EMAIL=noreply@consultant.com

# Local storage (when USE_AWS=false)
LOCAL_STORAGE_PATH=./storage

# Authentication
LEGACY_AUTH_ENABLED=true

# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_ISSUER=consultant-api
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d

# Security configuration
SECURITY_MAX_FAILED_LOGIN_ATTEMPTS=5
SECURITY_ACCOUNT_LOCK_DURATION=15m

# OIDC Configuration (disabled by default)
OIDC_ENABLED=false
OIDC_JWKS_TIMEOUT_SECONDS=10
```

### Secrets Management

For production deployments, use external secrets management:

```bash
# Using Infisical
npm install -g @infisical/cli
icli login
icli configure workspace your-workspace-id
icli secrets batch-add --env prod --secret-file .env.security
```

## Production Setup

### Database Setup

For production databases:

```bash
# External PostgreSQL setup
# Ensure the database is configured for production use
# Enable SSL/TLS connections
# Set up proper backup schedules
# Configure connection pooling
# Set up monitoring and alerting
```

### SSL/TLS Configuration

For HTTPS deployment:

```bash
# Generate SSL certificates
# Self-signed for development
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./certs/private.key \
  -out ./certs/certificate.crt

# Let's Encrypt for production
sudo certbot --nginx -d your-domain.com
```

### Load Balancing

For high availability:

```nginx
# nginx.conf
upstream consultant_api {
    server api1:8090;
    server api2:8090;
    server api3:8090;
}

server {
    listen 443 ssl;
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;

    location / {
        proxy_pass http://consultant_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Scaling

### Horizontal Scaling

The application is designed for horizontal scaling:

- **Stateless API**: No server-side session storage
- **Shared Database**: All instances share the same database
- **Load Balanced**: Designed to work behind load balancers
- **Auto-scaling**: Compatible with auto-scaling groups

### Database Scaling

For database scaling:

```bash
# Read replicas for read-heavy workloads
# Connection pooling optimization
# Query optimization
# Index tuning
# Partitioning for large tables
```

### Caching Strategy

Implement caching for better performance:

```bash
# Redis for session storage
# Application-level caching
# Database query caching
# CDN for static assets
```

## Monitoring and Logging

### Application Metrics

Enable metrics collection:

```bash
# JVM metrics
# API response times
# Error rates
# Throughput statistics
# Database connection pools
```

### Logging Configuration

Structured logging for production:

```bash
# Log levels: INFO, WARN, ERROR
# Structured JSON format
# Centralized log aggregation
# Retention policies
# Alerting on critical errors
```

### Health Checks

The application provides health check endpoints:

```bash
# API health check
GET /api/health

# Detailed health information
GET /api/health/details
```

## Security Considerations

### Production Security

Implement these security measures for production:

```bash
# Disable debug endpoints
# Enable security headers
# Configure proper CORS
# Implement rate limiting
# Secure secret management
# Regular security updates
```

### Certificate Management

For HTTPS certificates:

```bash
# Automated renewal for Let's Encrypt
# Certificate monitoring
# Backup certificate files
# Proper certificate chain
# OCSP stapling
```

## Deployment Scripts

### Automated Deployment

Create deployment scripts:

```bash
#!/bin/bash
# deploy.sh

# Pull latest code
git pull origin main

# Build the application
sbt clean assembly

# Run database migrations
sbt "data/flywayMigrate"

# Deploy to production
docker-compose up -d --build

echo "Deployment completed successfully"
```

### Rollback Procedure

Always have rollback procedures:

```bash
#!/bin/bash
# rollback.sh

# Rollback to previous version
docker-compose down
docker-compose run --rm --no-deps api:previous

# Or rollback database migrations
sbt "data/flywayRepair"
sbt "data/flywayMigrate -Dflyway.target=1.0.0"
```

## Frontend Integration

### Client Application

Configure frontend applications:

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      apiBase: 'https://your-domain.com/api'
    }
  }
})
```

### CORS Configuration

Set up proper CORS for frontend applications:

```bash
# Allow specific origins
ALLOWED_ORIGINS=http://localhost:3000,https://your-domain.com

# Allow credentials
ALLOW_CREDENTIALS=true

# Specify allowed methods
ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE
```

## Troubleshooting

### Common Issues

**Database Connection Issues**:
```bash
# Check if database is running
docker ps | grep postgres

# Verify connection parameters
docker logs consultant-db

# Test direct connection
PGPASSWORD=pass psql -h localhost -U user -d consultant -c "SELECT 1;"
```

**SSL Certificate Issues**:
```bash
# Verify certificate validity
openssl x509 -in certificate.crt -text -noout

# Check certificate expiration
openssl x509 -in certificate.crt -noout -dates
```

**Performance Issues**:
```bash
# Monitor application metrics
# Check database performance
# Verify connection pooling
# Review slow query logs
```

### Log Analysis

For log analysis:

```bash
# View application logs
docker logs consultant-api --tail 100

# Monitor logs in real-time
docker logs -f consultant-api

# Search for specific errors
docker logs consultant-api | grep ERROR
```

## Maintenance

### Regular Maintenance Tasks

- **Database Maintenance**: Vacuum and analyze tables
- **Log Rotation**: Rotate and compress log files
- **Backup Verification**: Regular backup restoration tests
- **Security Updates**: Regular security patching
- **Performance Tuning**: Monitor and tune performance

### Backup and Recovery

Implement comprehensive backup strategy:

```bash
# Database backups
pg_dump -h localhost -U consultant_user -d consultant > backup.sql

# Automated backup scripts
# Offsite backup storage
# Regular restore testing
# Point-in-time recovery
```

## Migration to AWS

The application is designed for easy migration to AWS:

```bash
# Enable AWS services
USE_AWS=true
AWS_REGION=us-east-1

# AWS Services used:
# - RDS for database
# - S3 for file storage
# - SES for email
# - SNS for notifications
# - SQS for messaging
# - EC2/ECS for compute
```

This configuration allows seamless migration from local PostgreSQL to AWS RDS, local file storage to S3, and local email services to AWS SES.