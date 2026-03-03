# Database Setup

This guide covers the database configuration and setup for the Consultant Backend.

## Database Configuration

The application uses PostgreSQL 16 as its primary database, managed with Flyway for migrations.

### Environment Variables

Database configuration is handled through environment variables in the `.env` file:

```bash
# Database configuration
DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://localhost:5432/consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass
DB_POOL_SIZE=32
```

### Automatic Initialization

The system automatically handles database initialization:

- All Flyway migrations run automatically on API startup
- Migrations are located in: `data/src/main/resources/db/migration/V*.sql`
- Each migration runs only once (tracked in `flyway_schema_history` table)
- Includes schema, test data, and constraints

## Quick Start Setup

### Using the Run Script (Recommended)

```bash
# This automatically:
# - Starts PostgreSQL in Docker
# - Runs all Flyway migrations
# - Seeds test data
./run.sh
```

### Manual Database Setup

If you prefer to set up the database manually:

```bash
# Start PostgreSQL in Docker
docker-compose up -d db

# Connect to the database
PGPASSWORD=consultant_pass psql -h localhost -U consultant_user -d consultant
```

## Migration Strategy

### Available Migrations

| Migration | Description |
|-----------|-------------|
| `V001__baseline_schema.sql` | Core database schema (tables, indexes, constraints) |
| `V002__seed_data.sql` | Test data and reference data (categories, connection types, test users) |
| `V003__add_system_settings.sql` | System settings table |

### Adding New Migrations

When adding new features, create migration files following the naming convention:

```bash
VXXX__descriptive_name.sql
```

Place migration files in: `data/src/main/resources/db/migration/`

## Database Schema

### Core Tables

- `users` - User accounts (clients, specialists, admins)
- `credentials` - Authentication credentials and tokens
- `specialists` - Specialist profiles and details
- `categories` - Hierarchical category system
- `consultations` - Consultation requests and status tracking
- `connection_types` - Available connection methods (WhatsApp, Viber, etc.)
- `specialist_connections` - Specialist contact information
- `specialist_category_rates` - Pricing and experience by category

### Indexes

The database includes optimized indexes:

- `idx_consultations_user` - For user-specific consultation queries
- `idx_consultations_specialist` - For specialist-specific consultation queries
- `idx_consultations_status` - For status-based filtering
- `idx_consultations_created` - For chronological queries

## Test Data

### Default Accounts

The system includes default test credentials for development:

| Account | Email | Password | Role | Phone |
|---------|-------|----------|------|-------|
| **Admin** | admin@admin.com | admin | Admin | N/A |
| **User** | user@example.com | user | Client | +1234567890 |
| **Specialist** | spec@example.com | spec | Specialist | +9876543210 |

### Default UUIDs

For testing purposes:

- Test User ID: `11111111-1111-1111-1111-111111111111`
- Test Specialist ID: `22222222-2222-2222-2222-222222222222`

## Production Considerations

### Security Warning

⚠️ **CRITICAL**: Change default passwords immediately after deploying to production!

### Connection Pooling

The application uses connection pooling with a default size of 32 connections. Adjust this based on your deployment needs:

```bash
DB_POOL_SIZE=64  # For higher traffic environments
```

### Backup Strategy

Implement regular database backups in production environments. The application supports standard PostgreSQL backup procedures.

## Troubleshooting

### Common Issues

**Database won't start:**
```bash
# Check if PostgreSQL container is running
docker ps | grep postgres

# View container logs
docker logs consultant-db-master
```

**Migration fails:**
```bash
# Check flyway history table
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;
```

**Connection issues:**
```bash
# Test direct database connection
PGPASSWORD=consultant_pass psql -h localhost -U consultant_user -d consultant -c "SELECT 1;"
```

### Cleaning Up

To reset the database completely:

```bash
# Stop containers
docker-compose down

# Remove volumes (destroys all data)
docker-compose down --volumes

# Start fresh
./run.sh
```

## Advanced Configuration

### Custom PostgreSQL Settings

You can customize PostgreSQL settings by modifying the Docker configuration. The default configuration includes optimizations for the application's needs.

### Monitoring

Monitor database performance using standard PostgreSQL tools and consider implementing connection pool monitoring in production environments.