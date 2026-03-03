# Development Setup

This guide covers setting up the development environment for the Consultant Backend.

## Prerequisites

### System Requirements

- **Operating System**: Linux, macOS, or Windows with WSL2
- **Docker**: Version 20.10 or higher
- **Java**: JDK 21 or higher
- **Build Tool**: sbt 1.9.8 or higher
- **Git**: Version control system
- **Node.js**: For frontend development (if needed)

### Verify Prerequisites

```bash
# Check Docker
docker --version
docker-compose version

# Check Java
java -version

# Check sbt
sbt --version

# Check Git
git --version

# Check Node.js (if developing frontends)
node --version
npm --version
```

## Initial Setup

### Clone the Repository

```bash
# Clone the repository
git clone https://github.com/lvn2000/consultant.git
cd consultant/backend
```

### Environment Configuration

Copy and configure the environment file:

```bash
# Copy the example environment file
cp .env.example .env

# Edit the environment variables as needed
nano .env
```

## Development Environment Setup

### Using the Run Script

The easiest way to start development:

```bash
# Start the complete development environment
./run.sh

# This will:
# - Load environment variables from .env
# - Start PostgreSQL in Docker
# - Run database migrations automatically
# - Start the API server on http://localhost:8090
```

### Manual Setup Steps

If you prefer manual setup:

```bash
# 1. Start the database
docker-compose up -d db

# 2. Wait for database to be ready
sleep 10

# 3. Run database migrations
sbt "data/flywayMigrate"

# 4. Start the API
sbt api/run
```

## IDE Configuration

### IntelliJ IDEA

1. Open the project directory
2. Import as sbt project
3. Install Scala plugin if not already installed
4. Configure JDK 21 as project SDK

### VS Code

Install recommended extensions:
- Metals (Scala Language Server)
- Scala Syntax
- sbt

## Code Structure

### Project Modules

```
backend/
├── api/                    # HTTP API layer (Tapir, Http4s)
├── core/                   # Domain models & business logic
├── data/                   # PostgreSQL repositories (Doobie)
├── infrastructure/         # AWS/Local adapters
├── scripts/                # Development & test scripts
└── target/                 # Build artifacts
```

### Frontend Applications

```
client-app/                 # Nuxt.js client frontend
admin-app/                  # Nuxt.js admin frontend
specialist-app/             # Nuxt.js specialist frontend
```

## Development Workflow

### Compile the Project

```bash
# Compile all modules
sbt compile

# Compile specific module
sbt core/compile
sbt api/compile
```

### Run Tests

```bash
# Run all tests
sbt test

# Run tests for specific module
sbt core/test
sbt api/test

# Run tests continuously (watch mode)
sbt ~test
```

### Continuous Compilation

```bash
# Auto-compile on file changes
sbt ~compile

# Auto-run with reload on changes
sbt api/reRun
```

## Database Development

### Schema Changes

When adding new features that require database changes:

1. Create a new migration file in `data/src/main/resources/db/migration/`
2. Follow the naming convention: `VXXX__descriptive_name.sql`
3. Write the SQL migration
4. Test the migration locally

### Sample Migration

```sql
-- V004__add_consultation_feedback.sql
ALTER TABLE consultations ADD COLUMN feedback TEXT;
ALTER TABLE consultations ADD COLUMN feedback_submitted BOOLEAN DEFAULT FALSE;
```

### Flyway Commands

```bash
# Check migration status
sbt "data/flywayStatus"

# Validate migrations
sbt "data/flywayValidate"

# Clean database (be careful!)
sbt "data/flywayClean"
```

## API Development

### Adding New Endpoints

1. Define the endpoint in `api/src/main/scala/com/consultant/api/routes/`
2. Use Tapir for type-safe endpoint definitions
3. Implement the server logic
4. Add to the routes collection

### Example Endpoint

```scala
// In ConsultationRoutes.scala
val createConsultationEndpoint = ApiEndpoints
  .securedEndpoint("createConsultation", "Create a new consultation")
  .post
  .in(jsonBody[CreateConsultationDto])
  .out(jsonBody[ConsultationDto])

val createConsultation = createConsultationEndpoint.serverLogic { dto =>
  consultationService.createConsultation(toCreateConsultationRequest(dto)).map {
    case Right(consultation) => Right(toConsultationDto(consultation))
    case Left(error)         => Left(toErrorResponse(error))
  }
}
```

## Testing

### Unit Tests

Located in `src/test/scala/` directories:

```bash
# Run all unit tests
sbt test

# Run specific test suite
sbt "testOnly *ServiceSpec"

# Run tests with coverage
sbt coverage test coverageReport
```

### Integration Tests

```bash
# Run integration tests
sbt "it:test"

# Run specific integration tests
sbt "it:testOnly *IntegrationSpec"
```

### API Testing Scripts

Use the provided test scripts:

```bash
# Test admin endpoints
./scripts/test-admin-count.sh

# Test specialist availability
./scripts/test-slots.sh

# Test specialist rates
./scripts/test-specialists-rates.sh
./scripts/test-specialists-response.sh
```

## Frontend Development

### Starting Frontend Applications

```bash
# Client app
cd client-app
npm install
npm run dev

# Admin app
cd admin-app
npm install
npm run dev

# Specialist app
cd specialist-app
npm install
npm run dev
```

### Frontend URLs

- **Client App**: http://localhost:3000
- **Admin App**: http://localhost:3001
- **Specialist App**: http://localhost:3002

## Debugging

### API Debugging

```bash
# Start with debug options
sbt -jvm-debug 5005 api/run

# Attach debugger to port 5005
```

### Database Debugging

```bash
# Connect to database directly
PGPASSWORD=consultant_pass psql -h localhost -U consultant_user -d consultant

# Run queries
SELECT * FROM users LIMIT 10;
```

### Log Configuration

Increase log levels for debugging:

```bash
# Set log level to DEBUG
export LOG_LEVEL=DEBUG

# Or configure in .env file
LOG_LEVEL=DEBUG
```

## Code Quality

### Formatting

The project uses scalafmt for code formatting:

```bash
# Format all code
sbt scalafmtAll

# Check formatting
sbt scalafmtCheckAll
```

### Static Analysis

```bash
# Run static analysis
sbt scapegoat

# Run scalastyle
sbt scalastyle
```

### Linting

```bash
# Check for potential issues
sbt wartremover
```

## Working with AWS Services

### Local Development

AWS services are disabled by default for local development:

```bash
# In .env
USE_AWS=false
```

### AWS Development Setup

For testing AWS integration locally:

```bash
# Enable AWS services
USE_AWS=true
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
```

## Common Development Tasks

### Adding Dependencies

Update `build.sbt`:

```scala
libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0"
```

Then reload:

```bash
sbt reload
```

### Running Migrations

```bash
# Apply pending migrations
sbt "data/flywayMigrate"

# Baseline an existing database
sbt "data/flywayBaseline"
```

### Seeding Test Data

```bash
# The application includes test data in V002__seed_data.sql
# This runs automatically with migrations
```

### Cleaning Build Artifacts

```bash
# Clean all build artifacts
sbt clean

# Clean specific module
sbt core/clean

# Clean and recompile
sbt clean compile
```

## Troubleshooting

### Common Issues

**Port Conflicts**:
```bash
# Check what's using port 8090
lsof -i :8090

# Kill process using port
kill -9 <PID>
```

**Docker Issues**:
```bash
# Check running containers
docker ps

# Remove stopped containers
docker container prune

# Check Docker logs
docker logs <container-name>
```

**Database Issues**:
```bash
# Check if database is running
docker ps | grep postgres

# Restart database
docker restart consultant-db-master
```

**Compilation Issues**:
```bash
# Clean and rebuild
sbt clean compile

# Check for dependency conflicts
sbt evicted
```

### Resetting Development Environment

```bash
# Stop all services
docker-compose down

# Remove volumes (this will delete all data)
docker-compose down --volumes

# Start fresh
./run.sh
```

## Performance Tips

### Speed Up Compilation

```bash
# Enable sbt server
sbt --client compile

# Use multiple cores
sbt -J-Xmx4G compile
```

### Database Performance

```bash
# Increase database connection pool size for development
DB_POOL_SIZE=16
```

### JVM Tuning

For development, you can tune JVM options:

```bash
# In .sbtopts file
-J-Xmx4G
-J-Xms1G
-J-XX:+UseG1GC
```

## Contributing

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes
# Commit with descriptive messages
git add .
git commit -m "Add feature: description of changes"

# Push and create PR
git push origin feature/my-feature
```

### Code Standards

- Follow the existing code style
- Write tests for new functionality
- Update documentation as needed
- Use meaningful commit messages
- Keep pull requests focused on single features

### Before Submitting PR

```bash
# Run all tests
sbt test

# Format code
sbt scalafmtAll

# Check for issues
sbt scapegoat
```

## Useful Commands

### Development Commands

```bash
# Start development server with auto-reload
sbt api/reRun

# Watch and compile on changes
sbt ~compile

# Run specific test
sbt "testOnly *ClassName*"

# Generate assembly JAR
sbt assembly

# Check dependency updates
sbt dependencyUpdates
```

### Database Commands

```bash
# View database schema
sbt "data/flywayInfo"

# Manually run a migration
sbt "data/flywayMigrate -Dflyway.target=1.0.1"

# Repair failed migration
sbt "data/flywayRepair"
```

This setup provides a complete development environment for working with the Consultant Backend application.