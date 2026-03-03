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
DB_USER=<your-db-user>
DB_PASSWORD=<your-strong-password>
DB_POOL_SIZE=32
```

🔒 **Security Note**: Never commit actual credentials to version control. Use `.env.example` as a template and create a local `.env` file (which should be gitignored).

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

# Connect to the database (replace with your credentials)
PGPASSWORD=<your-db-password> psql -h localhost -U <your-db-user> -d consultant
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

⚠️ **DEVELOPMENT ONLY**: The following test accounts are seeded by Flyway migration `V002__seed_data.sql` for development convenience. **NEVER use these in production!**

| Account | Email | Password | Role | Phone |
|---------|-------|----------|------|-------|
| **Admin** | admin@admin.com | `admin` | Admin | N/A |
| **User** | user@example.com | `user` | Client | +1234567890 |
| **Specialist** | spec@example.com | `spec` | Specialist | +9876543210 |

🔒 **Production Deployment**: 
- Disable test data seeding in production builds
- Create admin users through secure registration processes
- Use strong, unique passwords (minimum 12 characters with complexity requirements)
- See `WIKI_QUICK_START.md` for production security checklist

### Default UUIDs

For testing purposes:

- Test User ID: `11111111-1111-1111-1111-111111111111`
- Test Specialist ID: `22222222-2222-2222-2222-222222222222`

## Production Considerations

### Security Warning

⚠️ **CRITICAL**: Change default passwords immediately after deploying to production!

**Default Database Credentials (Development Only):**
- Database User: `consultant_user`
- Database Password: `consultant_pass`

🔒 **Production Requirements:**
- Use strong, unique database passwords (minimum 16 characters)
- Store credentials in secure secret management (e.g., AWS Secrets Manager, HashiCorp Vault)
- Never commit actual credentials to version control
- Use environment-specific configuration (`.env.production`, `.env.staging`)
- Implement database access logging and monitoring

### Connection Pooling

The application uses connection pooling with a default size of 32 connections. Adjust this based on your deployment needs:

```bash
DB_POOL_SIZE=64  # For higher traffic environments
DB_POOL_SIZE=100 # For high-traffic production deployments
DB_POOL_SIZE=200 # For very large deployments (use with caution)
```

**Recommended Values:**
- **Development**: 10-20 connections
- **Staging**: 20-32 connections  
- **Production (small)**: 32-64 connections
- **Production (medium)**: 64-100 connections
- **Production (large)**: 100-200 connections
- **Enterprise**: 200-500 connections (maximum allowed)

⚠️ **Important**: The maximum allowed pool size is 500. Values above this will be rejected at startup. Setting pool sizes too high can exhaust database resources and cause performance degradation.

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
# Test direct database connection (replace with your credentials)
PGPASSWORD=<your-db-password> psql -h localhost -U <your-db-user> -d consultant -c "SELECT 1;"
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