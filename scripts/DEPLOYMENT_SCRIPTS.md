# Refactored GCP Deployment Scripts

## Overview

The GCP deployment scripts have been refactored from a monolithic 876-line script into a modular, maintainable system with clear separation of concerns. This refactoring improves code quality, reusability, and maintainability while providing the same functionality.

## Quick Start

### Installation

The refactored scripts are located in the `scripts/` directory:

```bash
cd backend/scripts
chmod +x deploy-gcp-refactored.sh
```

### Basic Usage

```bash
# Deploy to development environment
./deploy-gcp-refactored.sh dev

# Deploy to staging with custom domain
./deploy-gcp-refactored.sh staging --domain=staging.myapp.com

# Deploy to production
./deploy-gcp-refactored.sh prod

# Dry run (simulate without making changes)
./deploy-gcp-refactored.sh prod --dry-run
```

## Architecture

### Directory Structure

```
backend/scripts/
├── deploy-gcp-refactored.sh          # Main deployment script (entry point)
├── lib/
│   ├── logging.sh                    # Logging utilities
│   ├── config.sh                     # Configuration management
│   ├── gcp.sh                        # GCP API utilities
│   └── deployment.sh                 # Deployment orchestration
├── config/
│   ├── dev.env                       # Development configuration
│   ├── staging.env                   # Staging configuration
│   └── prod.env                      # Production configuration
├── DEPLOYMENT_SCRIPTS.md             # This file
└── README.md                         # Scripts overview
```

## Modules

### 1. logging.sh

Provides centralized logging with configurable levels and formatting.

**Log Levels:**
- `DEBUG` (0) - Detailed debugging information
- `INFO` (1) - General informational messages (default)
- `WARNING` (2) - Warning messages for non-critical issues
- `ERROR` (3) - Error messages and script termination

**Functions:**

```bash
log_debug "Debug message"              # Only shown in DEBUG mode
log_info "Information message"         # Standard information
log_success "Success message"          # Success message (green)
log_warning "Warning message"          # Warning message (yellow)
log_error "Error message"              # Error and exit
log_error_continue "Error message"     # Error without exiting
log_section "Section Title"            # Section header
log_step 1 "Step description"          # Numbered step
log_variable "NAME" "value"            # Format variable display
```

**Configuration:**

```bash
export LOG_LEVEL=0                     # 0=DEBUG, 1=INFO, 2=WARNING, 3=ERROR
export LOG_WITH_TIMESTAMPS=true        # Enable/disable timestamps
```

**Features:**
- Color-coded output by level
- Optional timestamps
- Standardized formatting
- Automatic error exit handling

### 2. config.sh

Manages application configuration with environment-specific settings.

**Key Responsibilities:**
- Environment validation (dev/staging/prod)
- Service naming conventions
- Configuration defaults by environment
- GCP resource name generation
- Configuration file loading

**Environment-Specific Defaults:**

```bash
# Development
ENVIRONMENT=dev
DATABASE_TIER=db-f1-micro
ENABLE_BACKUP=false
ENABLE_MONITORING=false

# Staging
ENVIRONMENT=staging
DATABASE_TIER=db-g1-small
ENABLE_BACKUP=true
ENABLE_MONITORING=true

# Production
ENVIRONMENT=prod
DATABASE_TIER=db-custom-2-7680
ENABLE_BACKUP=true
ENABLE_MONITORING=true
```

**Key Functions:**

```bash
get_env_config "database_tier"              # Get config by key
get_service_name "backend"                  # Get service names
validate_environment                        # Validate env setting
get_database_instance_name                  # Database instance name
get_artifact_registry_path "backend"        # Registry path
get_vpc_connector_name                      # VPC connector name
print_configuration                         # Display config
init_config                                 # Initialize & validate
```

**Configuration Variables:**

```bash
ENVIRONMENT                # Deployment environment
GCP_PROJECT_ID             # GCP project identifier
FORCE_HTTPS                # Enable HTTPS enforcement
ENABLE_CUSTOM_DOMAINS      # Enable custom domain setup
SKIP_DATABASE_SETUP        # Skip database creation
SKIP_BUILDS                # Skip Docker image builds
DRY_RUN                    # Simulate without changes
CUSTOM_DOMAIN              # Domain for all services
BACKEND_DOMAIN             # Backend-specific domain
CLIENT_DOMAIN              # Client app domain
ADMIN_DOMAIN               # Admin app domain
SPECIALIST_DOMAIN          # Specialist app domain
```

### 3. gcp.sh

Provides utilities for interacting with Google Cloud Platform.

**Function Groups:**

#### Authentication & Project Setup
```bash
check_gcloud_auth              # Verify gcloud authentication
set_gcp_project "project-id"   # Set active GCP project
get_active_project             # Get current project
enable_gcp_apis                # Enable required APIs
```

#### Service Accounts
```bash
create_service_account "name" "display-name"   # Create service account
grant_iam_role "account" "role"                # Grant IAM role
```

#### Artifact Registry
```bash
create_artifact_registry "repo" "region"          # Create repository
push_docker_image "local:tag" "registry:tag"     # Push Docker image
```

#### Cloud SQL
```bash
create_cloud_sql_instance "name" "tier" "region"    # Create instance
create_cloud_sql_database "instance" "database"     # Create database
create_cloud_sql_user "instance" "user" "password"  # Create user
```

#### Cloud Run
```bash
deploy_cloud_run_service "name" "image" "region" "memory"   # Deploy
set_cloud_run_env_vars "service" "region" "VAR=value"       # Set env vars
get_service_url "service" "region"                          # Get URL
```

#### Networking
```bash
create_vpc_connector "name" "network" "subnet" "region"      # Create VPC
map_custom_domain "service" "domain" "region"                # Map domain
```

**Features:**
- Comprehensive gcloud CLI wrappers
- Error handling and validation
- Dry-run support for all operations
- Automatic resource existence checks

### 4. deployment.sh

Orchestrates the complete deployment process.

**Deployment Phases:**

1. **Prerequisites Check**
   - Verify required tools (gcloud, docker, git, kubectl)
   - Check gcloud authentication
   - Verify GCP project access

2. **Image Building**
   - Build backend Docker image
   - Build frontend Docker images
   - Push images to Artifact Registry

3. **Database Setup**
   - Create Cloud SQL instance
   - Create database and users
   - Run database migrations

4. **Networking**
   - Create VPC connector
   - Configure private networking

5. **Service Deployment**
   - Deploy backend to Cloud Run
   - Deploy frontend apps to Cloud Run
   - Configure environment variables

6. **Domain Configuration**
   - Map custom domains
   - Configure SSL/TLS certificates

7. **Verification**
   - Display deployment summary
   - Show service URLs

**Key Functions:**

```bash
check_prerequisites              # Verify requirements
build_backend_image "name"       # Build backend
build_frontend_images "path"     # Build frontends
push_images_to_registry          # Push all images
setup_database                   # Setup Cloud SQL
setup_networking                 # Setup VPC
deploy_backend_service           # Deploy backend
deploy_frontend_services         # Deploy frontends
execute_deployment               # Main orchestration
display_deployment_summary       # Show results
```

## Command Line Options

### Basic Options

```bash
# Environment specification
./deploy-gcp-refactored.sh dev          # Development
./deploy-gcp-refactored.sh staging      # Staging
./deploy-gcp-refactored.sh prod         # Production
```

### Domain Configuration

```bash
# Single domain for all services
./deploy-gcp-refactored.sh prod --domain=myapp.com

# Individual service domains
./deploy-gcp-refactored.sh prod \
  --backend-domain=api.myapp.com \
  --client-domain=app.myapp.com \
  --admin-domain=admin.myapp.com \
  --specialist-domain=specialist.myapp.com
```

### Operational Flags

```bash
# Skip specific steps
./deploy-gcp-refactored.sh dev --skip-builds      # Skip Docker builds
./deploy-gcp-refactored.sh dev --skip-database    # Skip DB setup
./deploy-gcp-refactored.sh dev --skip-domains     # Skip domain setup

# Testing and debugging
./deploy-gcp-refactored.sh prod --dry-run         # Simulate only
./deploy-gcp-refactored.sh dev --no-https         # Disable HTTPS
```

### Logging Options

```bash
# Debug mode
./deploy-gcp-refactored.sh dev --log-level=DEBUG

# With timestamps
./deploy-gcp-refactored.sh dev --with-timestamps

# Combined
./deploy-gcp-refactored.sh dev --log-level=DEBUG --with-timestamps
```

### Help

```bash
./deploy-gcp-refactored.sh --help    # Show full help message
./deploy-gcp-refactored.sh -h        # Short help
```

## Usage Examples

### Development Deployment

```bash
# Deploy to development with debug logging
./deploy-gcp-refactored.sh dev --log-level=DEBUG --with-timestamps

# Dry run first to verify configuration
./deploy-gcp-refactored.sh dev --dry-run

# Skip Docker builds if images already exist
./deploy-gcp-refactored.sh dev --skip-builds
```

### Staging Deployment

```bash
# Deploy to staging with custom domain
./deploy-gcp-refactored.sh staging --domain=staging.myapp.com

# Deploy specific domains
./deploy-gcp-refactored.sh staging \
  --backend-domain=api-staging.myapp.com \
  --client-domain=app-staging.myapp.com
```

### Production Deployment

```bash
# Full production deployment
./deploy-gcp-refactored.sh prod

# Dry run to verify before actual deployment
./deploy-gcp-refactored.sh prod --dry-run

# Production with custom domains
./deploy-gcp-refactored.sh prod \
  --backend-domain=api.myapp.com \
  --client-domain=app.myapp.com \
  --admin-domain=admin.myapp.com
```

## Environment Variables

### Required

```bash
export GCP_PROJECT_ID="my-project-prod"
```

### Optional

```bash
export ENVIRONMENT="prod"                  # Override command line
export LOG_LEVEL=0                         # 0=DEBUG, 1=INFO, 2=WARNING, 3=ERROR
export LOG_WITH_TIMESTAMPS=true            # Enable timestamps
export DRY_RUN=true                        # Simulate deployment
export FORCE_HTTPS=true                    # Enforce HTTPS
export SKIP_DATABASE_SETUP=false           # Skip DB creation
export SKIP_BUILDS=false                   # Skip Docker builds
export CUSTOM_DOMAIN="myapp.com"           # Domain for all services
export BACKEND_DOMAIN="api.myapp.com"      # Backend domain
export CLIENT_DOMAIN="app.myapp.com"       # Client domain
export ADMIN_DOMAIN="admin.myapp.com"      # Admin domain
export BACKEND_MEMORY="2Gi"                # Backend memory limit
export FRONTEND_MEMORY="1Gi"               # Frontend memory limit
```

## Configuration Files

Create environment-specific configuration files in `scripts/config/`:

### dev.env

```bash
# Development Environment Configuration
ENVIRONMENT=dev
GCP_PROJECT_ID=my-project-dev
REGION=us-central1
DATABASE_TIER=db-f1-micro
ENABLE_BACKUP=false
ENABLE_MONITORING=false
FORCE_HTTPS=true
BACKEND_MEMORY=512Mi
FRONTEND_MEMORY=256Mi
```

### staging.env

```bash
# Staging Environment Configuration
ENVIRONMENT=staging
GCP_PROJECT_ID=my-project-staging
REGION=us-central1
DATABASE_TIER=db-g1-small
ENABLE_BACKUP=true
ENABLE_MONITORING=true
FORCE_HTTPS=true
BACKEND_MEMORY=1Gi
FRONTEND_MEMORY=512Mi
```

### prod.env

```bash
# Production Environment Configuration
ENVIRONMENT=prod
GCP_PROJECT_ID=my-project-prod
REGION=us-central1
DATABASE_TIER=db-custom-2-7680
ENABLE_BACKUP=true
ENABLE_MONITORING=true
FORCE_HTTPS=true
BACKEND_MEMORY=2Gi
FRONTEND_MEMORY=1Gi
GCP_ENABLE_VPC_CONNECTOR=true
```

## Dry Run Mode

Test deployments without making actual changes:

```bash
# Simulate deployment
./deploy-gcp-refactored.sh prod --dry-run

# Output shows what would be executed
[INFO] [DRY RUN] Would execute: gcloud config set project my-project-prod
[INFO] [DRY RUN] Would create Cloud SQL instance: consultant-postgres-prod
[INFO] [DRY RUN] Would build image: docker build -t consultant-backend...
```

## Error Handling

The scripts implement comprehensive error handling:

### Pre-flight Validation
- Check for required tools
- Verify GCP authentication
- Validate configuration
- Check project access

### Execution Safety
- Fail fast on errors
- Provide helpful error messages
- Show stack traces in debug mode
- Continue on non-critical errors

### Recovery Tips

**"gcloud CLI is not installed"**
- Solution: Install Google Cloud SDK
- Reference: https://cloud.google.com/sdk/docs/install

**"No active gcloud authentication"**
- Solution: Run `gcloud auth login`
- Reference: https://cloud.google.com/docs/authentication

**"Invalid GCP_PROJECT_ID format"**
- Solution: Use lowercase letters, numbers, and hyphens only
- Example: `my-project-prod` (valid), `MyProject` (invalid)

**"Failed to enable APIs"**
- Solution: Verify IAM permissions in GCP console
- Check: Project Editor or appropriate roles

**"Failed to create Cloud SQL instance"**
- Solution: Check quota limits in GCP console
- Verify billing account is active

## Migration from Original Script

### Backward Compatibility

The original `deploy-gcp.sh` remains available. To migrate:

1. **Test Phase**: Run refactored script in dev environment
   ```bash
   ./scripts/deploy-gcp-refactored.sh dev --dry-run
   ```

2. **Validation Phase**: Compare logs with original script
   ```bash
   # Original
   ./deploy-gcp.sh dev

   # Refactored
   ./scripts/deploy-gcp-refactored.sh dev
   ```

3. **Production Phase**: Switch to refactored script after testing
   ```bash
   ./scripts/deploy-gcp-refactored.sh prod
   ```

4. **Archive Phase**: Keep original for reference
   ```bash
   cp deploy-gcp.sh deploy-gcp.sh.backup
   ```

## Best Practices

### Security

1. **Never commit credentials**
   - Use GCP Secret Manager for sensitive data
   - Set environment variables securely

2. **Use service accounts**
   - Create service accounts per environment
   - Grant minimal required permissions (least privilege)

3. **HTTPS enforcement**
   - Always use `--force-https=true` in production
   - Use managed SSL certificates

### Performance

1. **Optimize build times**
   - Use `--skip-builds` if images are cached
   - Consider multi-stage Dockerfile builds

2. **Parallel deployments**
   - Deploy multiple services concurrently
   - Use Cloud Build for faster builds

3. **Resource allocation**
   - Set appropriate memory limits per service
   - Use auto-scaling for variable loads

### Reliability

1. **Always dry-run first**
   ```bash
   ./deploy-gcp-refactored.sh prod --dry-run
   ```

2. **Test in dev/staging first**
   - Verify all changes work before production
   - Use consistent configurations across environments

3. **Monitor deployments**
   - Check Cloud Run logs after deployment
   - Verify service health endpoints

### Debugging

1. **Enable debug logging**
   ```bash
   ./deploy-gcp-refactored.sh dev --log-level=DEBUG
   ```

2. **Check GCP console**
   - Cloud Run service details
   - Cloud SQL instance status
   - Cloud Build logs

3. **Review error messages**
   - Read full error output
   - Check prerequisites section

## Troubleshooting

### Common Issues

**Deployment fails during image build**
- Check Docker daemon is running
- Verify Dockerfile exists and is valid
- Check available disk space

**Cloud SQL creation fails**
- Verify billing account is active
- Check quota limits for Cloud SQL
- Ensure sufficient permissions

**Domain mapping fails**
- Verify domain DNS records
- Check domain is properly configured in GCP
- Verify SSL certificate is valid

**Service deployment fails**
- Check image exists in Artifact Registry
- Verify service account has permissions
- Check Cloud Run quota limits

### Getting Help

1. **Check logs** with debug mode
   ```bash
   ./deploy-gcp-refactored.sh dev --log-level=DEBUG --with-timestamps
   ```

2. **Review GCP Cloud Console**
   - Check service status
   - Review error logs
   - Check quota usage

3. **Consult documentation**
   - See DEPLOYMENT_REFACTORING.md for detailed module docs
   - Check GCP_DEPLOYMENT.md for GCP-specific guidance

## Version History

- **v2.0** (Current) - Refactored with modular architecture
- **v1.0** - Original monolithic script

## Future Enhancements

- [ ] Kubernetes/GKE support
- [ ] Multi-region deployments
- [ ] Automated rollback functionality
- [ ] Cost estimation
- [ ] Health checks and monitoring
- [ ] Automated backups
- [ ] Integration tests
- [ ] Performance optimization

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review deployment logs with debug mode
3. Consult GCP documentation
4. Contact DevOps team
