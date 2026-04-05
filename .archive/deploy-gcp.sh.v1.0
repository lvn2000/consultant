#!/bin/bash

# ============================================
# GCP Deployment Script for Consultant Platform
# ============================================
# Deploys the full Consultant application to Google Cloud Platform:
# - Cloud SQL (PostgreSQL) for database
# - Cloud Run for backend API (Scala/Http4s)
# - Cloud Run for frontend apps (Nuxt.js)
# - Automatic HTTPS with Google-managed certificates
# - Custom domain support with SSL/TLS
#
# Architecture:
# Google Cloud Project
# ├── Cloud Run Service: admin-app
# ├── Cloud Run Service: specialist-app
# ├── Cloud Run Service: client-app
# ├── Cloud Run Service: backend
# └── Cloud SQL (PostgreSQL)
#
# Usage: ./deploy-gcp.sh [environment] [options]
#   environment: dev, staging, prod (default: dev)
#   options:
#     --domain=DOMAIN          Custom domain (e.g., api.yourdomain.com)
#     --backend-domain=DOMAIN  Custom domain for backend
#     --client-domain=DOMAIN   Custom domain for client app
#     --admin-domain=DOMAIN    Custom domain for admin app
#     --specialist-domain=DOMAIN Custom domain for specialist app
#     --no-https               Disable HTTPS enforcement (not recommended)
#     --skip-domains           Skip custom domain setup
# ============================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Configuration
ENVIRONMENT="dev"
FORCE_HTTPS="true"
ENABLE_CUSTOM_DOMAINS="false"
SKIP_DOMAINS="false"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Domain configuration
CUSTOM_DOMAIN=""
BACKEND_DOMAIN=""
CLIENT_DOMAIN=""
ADMIN_DOMAIN=""
SPECIALIST_DOMAIN=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        dev|staging|prod)
            ENVIRONMENT="$1"
            shift
            ;;
        --domain=*)
            CUSTOM_DOMAIN="${1#*=}"
            ENABLE_CUSTOM_DOMAINS="true"
            shift
            ;;
        --backend-domain=*)
            BACKEND_DOMAIN="${1#*=}"
            ENABLE_CUSTOM_DOMAINS="true"
            shift
            ;;
        --client-domain=*)
            CLIENT_DOMAIN="${1#*=}"
            ENABLE_CUSTOM_DOMAINS="true"
            shift
            ;;
        --admin-domain=*)
            ADMIN_DOMAIN="${1#*=}"
            ENABLE_CUSTOM_DOMAINS="true"
            shift
            ;;
        --specialist-domain=*)
            SPECIALIST_DOMAIN="${1#*=}"
            ENABLE_CUSTOM_DOMAINS="true"
            shift
            ;;
        --no-https)
            FORCE_HTTPS="false"
            shift
            ;;
        --skip-domains)
            SKIP_DOMAINS="true"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [environment] [options]"
            echo "  environment: dev, staging, prod (default: dev)"
            echo ""
            echo "Options:"
            echo "  --domain=DOMAIN          Custom domain for all services"
            echo "  --backend-domain=DOMAIN  Custom domain for backend service"
            echo "  --client-domain=DOMAIN   Custom domain for client app"
            echo "  --admin-domain=DOMAIN    Custom domain for admin app"
            echo "  --specialist-domain=DOMAIN Custom domain for specialist app"
            echo "  --no-https               Disable HTTPS enforcement (not recommended)"
            echo "  --skip-domains           Skip custom domain setup"
            echo "  -h, --help               Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  GCP_PROJECT_ID      - GCP Project ID (optional)"
            echo "  GCP_BILLING_ACCOUNT - Billing account ID (optional)"
            exit 0
            ;;
        *)
            log_error "Unknown argument: $1"
            ;;
    esac
done

# Default configuration based on environment
case "$ENVIRONMENT" in
    dev)
        GCP_PROJECT_SUFFIX="-dev"
        REGION="us-central1"
        DATABASE_TIER="db-f1-micro"
        SERVICE_ACCOUNT_SUFFIX="-dev"
        ;;
    staging)
        GCP_PROJECT_SUFFIX="-staging"
        REGION="us-central1"
        DATABASE_TIER="db-g1-small"
        SERVICE_ACCOUNT_SUFFIX="-staging"
        ;;
    prod)
        GCP_PROJECT_SUFFIX=""
        REGION="us-central1"
        DATABASE_TIER="db-custom-2-7680"
        SERVICE_ACCOUNT_SUFFIX=""
        ;;
    *)
        log_error "Invalid environment: $ENVIRONMENT. Use dev, staging, or prod."
        ;;
esac

# Ask for GCP project ID if not set
if [ -z "${GCP_PROJECT_ID:-}" ]; then
    echo -n "Enter your GCP Project ID (leave empty to create new): "
    read -r USER_PROJECT_ID
    if [ -n "$USER_PROJECT_ID" ]; then
        GCP_PROJECT_ID="$USER_PROJECT_ID"
    else
        echo -n "Enter desired project name (will be suffixed with $GCP_PROJECT_SUFFIX): "
        read -r PROJECT_NAME
        GCP_PROJECT_ID="${PROJECT_NAME}${GCP_PROJECT_SUFFIX}"
    fi
fi

# Application configuration
APP_NAME="consultant"
BACKEND_SERVICE="${APP_NAME}-backend"
CLIENT_APP_SERVICE="${APP_NAME}-client-app"
ADMIN_APP_SERVICE="${APP_NAME}-admin-app"
SPECIALIST_APP_SERVICE="${APP_NAME}-specialist-app"
DATABASE_INSTANCE="${APP_NAME}-postgres-${ENVIRONMENT}"
DATABASE_NAME="consultant"
DATABASE_USER="consultant_user"
ARTIFACT_REGISTRY="${REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${APP_NAME}"
VPC_CONNECTOR="${APP_NAME}-connector"

# Set default domains if custom domain provided but individual domains not set
if [ -n "$CUSTOM_DOMAIN" ]; then
    if [ -z "$BACKEND_DOMAIN" ]; then
        BACKEND_DOMAIN="api.${CUSTOM_DOMAIN}"
    fi
    if [ -z "$CLIENT_DOMAIN" ]; then
        CLIENT_DOMAIN="app.${CUSTOM_DOMAIN}"
    fi
    if [ -z "$ADMIN_DOMAIN" ]; then
        ADMIN_DOMAIN="admin.${CUSTOM_DOMAIN}"
    fi
    if [ -z "$SPECIALIST_DOMAIN" ]; then
        SPECIALIST_DOMAIN="specialist.${CUSTOM_DOMAIN}"
    fi
fi

# Function to generate random string with fallback
generate_random_string() {
    local length=$1
    # Try openssl first
    if command -v openssl &> /dev/null; then
        openssl rand -base64 $((length * 2)) | tr -dc 'a-zA-Z0-9' | head -c "$length"
    # Fallback to /dev/urandom
    elif [ -c /dev/urandom ]; then
        tr -dc 'a-zA-Z0-9' < /dev/urandom | head -c "$length"
    # Final fallback to date +%s%N (less secure but works)
    else
        date +%s%N | md5sum | head -c "$length"
    fi
}

# Generated passwords with fallback
DATABASE_PASSWORD=$(generate_random_string 32)
JWT_SECRET=$(generate_random_string 64)
ENCRYPTION_KEY=$(generate_random_string 32)

# ============================================
# Prerequisite Checks
# ============================================

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check gcloud CLI
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI is not installed. Please install Google Cloud SDK: https://cloud.google.com/sdk/docs/install"
    fi

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker: https://docs.docker.com/get-docker/"
    fi

    # Check if authenticated with gcloud
    if ! gcloud auth list --format="value(account)" | grep -q "@"; then
        log_warning "Not authenticated with gcloud. Running 'gcloud auth login'..."
        gcloud auth login
    fi

    log_success "Prerequisites check passed"
}

# ============================================
# GCP Project Setup
# ============================================

setup_gcp_project() {
    log_info "Setting up GCP project: $GCP_PROJECT_ID"

    # Check if project exists
    if gcloud projects describe "$GCP_PROJECT_ID" &>/dev/null; then
        log_info "Project $GCP_PROJECT_ID already exists"
    else
        log_info "Creating new project: $GCP_PROJECT_ID"
        gcloud projects create "$GCP_PROJECT_ID" --name="${APP_NAME}-${ENVIRONMENT}"
    fi

    # Set project as active
    gcloud config set project "$GCP_PROJECT_ID"

    # Enable required APIs
    log_info "Enabling required GCP APIs..."
    gcloud services enable \
        cloudresourcemanager.googleapis.com \
        serviceusage.googleapis.com \
        cloudbuild.googleapis.com \
        run.googleapis.com \
        sqladmin.googleapis.com \
        secretmanager.googleapis.com \
        artifactregistry.googleapis.com \
        vpcaccess.googleapis.com \
        compute.googleapis.com \
        domains.googleapis.com \
        dns.googleapis.com \
        --quiet

    # Link billing account if not already linked
    if [ -n "${GCP_BILLING_ACCOUNT:-}" ]; then
        log_info "Linking billing account..."
        gcloud beta billing projects link "$GCP_PROJECT_ID" \
            --billing-account="$GCP_BILLING_ACCOUNT" \
            --quiet
    else
        log_warning "GCP_BILLING_ACCOUNT not set. You may need to link billing account manually."
        log_warning "Run: gcloud beta billing projects link $GCP_PROJECT_ID --billing-account=YOUR_BILLING_ACCOUNT_ID"
    fi

    log_success "GCP project setup complete"
}

# ============================================
# Artifact Registry Setup
# ============================================

setup_artifact_registry() {
    log_info "Setting up Artifact Registry..."

    # Create repository if it doesn't exist
    if ! gcloud artifacts repositories describe "$APP_NAME" --location="$REGION" &>/dev/null; then
        gcloud artifacts repositories create "$APP_NAME" \
            --repository-format=docker \
            --location="$REGION" \
            --description="Docker images for ${APP_NAME} application" \
            --quiet
    fi

    # Configure Docker to use Artifact Registry
    gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet

    log_success "Artifact Registry setup complete"
}

# ============================================
# Cloud SQL Database Setup
# ============================================

setup_database() {
    log_info "Setting up Cloud SQL PostgreSQL database..."

    # Check if instance already exists
    if gcloud sql instances describe "$DATABASE_INSTANCE" &>/dev/null; then
        log_info "Database instance $DATABASE_INSTANCE already exists"
    else
        log_info "Creating Cloud SQL instance: $DATABASE_INSTANCE"
        gcloud sql instances create "$DATABASE_INSTANCE" \
            --database-version=POSTGRES_16 \
            --tier="$DATABASE_TIER" \
            --region="$REGION" \
            --storage-size=100 \
            --storage-type=SSD \
            --backup-start-time=02:00 \
            --maintenance-window-day=MON \
            --maintenance-window-hour=03 \
            --availability-type=zonal \
            --database-flags=cloudsql.iam_authentication=on \
            --quiet

        # Set root password
        gcloud sql users set-password postgres \
            --instance="$DATABASE_INSTANCE" \
            --password="$DATABASE_PASSWORD" \
            --quiet
    fi

    # Create application database
    log_info "Creating database: $DATABASE_NAME"
    gcloud sql databases create "$DATABASE_NAME" \
        --instance="$DATABASE_INSTANCE" \
        --quiet || log_warning "Database may already exist"

    # Create application user
    log_info "Creating database user: $DATABASE_USER"
    gcloud sql users create "$DATABASE_USER" \
        --instance="$DATABASE_INSTANCE" \
        --password="$DATABASE_PASSWORD" \
        --quiet || log_warning "User may already exist"

    # Get database connection info
    DATABASE_PUBLIC_IP=$(gcloud sql instances describe "$DATABASE_INSTANCE" \
        --format="value(ipAddresses[0].ipAddress)")
    DATABASE_PRIVATE_IP=$(gcloud sql instances describe "$DATABASE_INSTANCE" \
        --format="value(ipAddresses[1].ipAddress)" 2>/dev/null || echo "")

    # Create connection string
    if [ -n "$DATABASE_PRIVATE_IP" ]; then
        DATABASE_URL="jdbc:postgresql://${DATABASE_PRIVATE_IP}:5432/${DATABASE_NAME}"
        log_info "Using private IP for database connection"
    else
        DATABASE_URL="jdbc:postgresql://${DATABASE_PUBLIC_IP}:5432/${DATABASE_NAME}"
        log_info "Using public IP for database connection (consider setting up private IP)"
    fi

    log_success "Database setup complete"
    log_info "Database URL: ${DATABASE_URL}"
}

# ============================================
# VPC Connector Setup (for private database access)
# ============================================

setup_vpc_connector() {
    log_info "Setting up VPC Connector for private database access..."

    # Check if VPC connector exists
    if gcloud compute networks vpc-access connectors describe "$VPC_CONNECTOR" \
        --region="$REGION" &>/dev/null; then
        log_info "VPC Connector $VPC_CONNECTOR already exists"
    else
        log_info "Creating VPC Connector: $VPC_CONNECTOR"
        gcloud compute networks vpc-access connectors create "$VPC_CONNECTOR" \
            --region="$REGION" \
            --range="10.8.0.0/28" \
            --network=default \
            --quiet
    fi

    log_success "VPC Connector setup complete"
}

# ============================================
# Secret Manager Setup
# ============================================

setup_secrets() {
    log_info "Setting up Secret Manager..."

    # Create secrets
    secrets=(
        "consultant-db-password:$DATABASE_PASSWORD"
        "consultant-jwt-secret:$JWT_SECRET"
        "consultant-encryption-key:$ENCRYPTION_KEY"
    )

    for secret in "${secrets[@]}"; do
        IFS=':' read -r secret_name secret_value <<< "$secret"
        full_secret_name="${secret_name}-${ENVIRONMENT}"

        # Check if secret exists
        if ! gcloud secrets describe "$full_secret_name" &>/dev/null; then
            echo "$secret_value" | gcloud secrets create "$full_secret_name" \
                --data-file=- \
                --replication-policy=automatic \
                --quiet
            log_info "Created secret: $full_secret_name"
        else
            log_info "Secret $full_secret_name already exists"
        fi
    done

    log_success "Secret Manager setup complete"
}

# ============================================
# Docker Image Building
# ============================================

build_and_push_images() {
    log_info "Building and pushing Docker images..."

    # Build backend image
    log_info "Building backend Docker image..."
    docker build -t "$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:latest" \
        -t "$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:${ENVIRONMENT}" \
        -f "$PROJECT_ROOT/Dockerfile" \
        "$PROJECT_ROOT"

    # Push backend image
    log_info "Pushing backend image to Artifact Registry..."
    docker push "$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:latest"
    docker push "$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:${ENVIRONMENT}"

    # Build frontend images
    frontend_apps=("client-app" "admin-app" "specialist-app")

    for app in "${frontend_apps[@]}"; do
        service_name="${APP_NAME}-${app}"
        log_info "Building $app Docker image..."

        docker build -t "$ARTIFACT_REGISTRY/${service_name}:latest" \
            -t "$ARTIFACT_REGISTRY/${service_name}:${ENVIRONMENT}" \
            --build-arg "APP_DIR=${app}" \
            -f "$PROJECT_ROOT/frontend.Dockerfile" \
            "$PROJECT_ROOT"

        log_info "Pushing $app image to Artifact Registry..."
        docker push "$ARTIFACT_REGISTRY/${service_name}:latest"
        docker push "$ARTIFACT_REGISTRY/${service_name}:${ENVIRONMENT}"
    done

    log_success "All Docker images built and pushed"
}

# ============================================
# Backend Service Deployment with HTTPS
# ============================================

deploy_backend() {
    log_info "Deploying backend service to Cloud Run..."

    # Get database connection details
    DATABASE_PASSWORD_SECRET=$(gcloud secrets versions access latest \
        --secret="consultant-db-password-${ENVIRONMENT}")
    JWT_SECRET_SECRET=$(gcloud secrets versions access latest \
        --secret="consultant-jwt-secret-${ENVIRONMENT}")
    ENCRYPTION_KEY_SECRET=$(gcloud secrets versions access latest \
        --secret="consultant-encryption-key-${ENVIRONMENT}")

    # Security configuration based on HTTPS setting
    SECURE_COOKIES="true"
    SESSION_SECURE="true"
    if [ "$FORCE_HTTPS" = "false" ]; then
        SECURE_COOKIES="false"
        SESSION_SECURE="false"
        log_warning "HTTPS enforcement disabled. This is not recommended for production."
    fi

    # Deploy to Cloud Run
    gcloud run deploy "$BACKEND_SERVICE" \
        --image="$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:${ENVIRONMENT}" \
        --region="$REGION" \
        --platform="managed" \
        --allow-unauthenticated \
        --memory="1Gi" \
        --cpu="1" \
        --max-instances=10 \
        --min-instances=1 \
        --concurrency=80 \
        --timeout=300 \
        --vpc-connector="$VPC_CONNECTOR" \
        --set-env-vars="\
            ENVIRONMENT=${ENVIRONMENT},\
            SERVER_HOST=0.0.0.0,\
            SERVER_PORT=8090,\
            DB_DRIVER=org.postgresql.Driver,\
            DB_URL=${DATABASE_URL},\
            DB_USER=${DATABASE_USER},\
            DB_PASSWORD=${DATABASE_PASSWORD_SECRET},\
            DB_POOL_SIZE=32,\
            JWT_SECRET=${JWT_SECRET_SECRET},\
            JWT_ISSUER=consultant-api-${ENVIRONMENT},\
            JWT_ACCESS_TTL=15m,\
            JWT_REFRESH_TTL=7d,\
            DB_ENCRYPTION_KEY=${ENCRYPTION_KEY_SECRET},\
            USE_AWS=false,\
            FORCE_HTTPS=${FORCE_HTTPS},\
            SECURE_COOKIES=${SECURE_COOKIES},\
            SESSION_SECURE=${SESSION_SECURE},\
            ENABLE_HTTP2=true,\
            SECURITY_HEADERS=true" \
        --quiet

    # Get backend service URL
    BACKEND_URL=$(gcloud run services describe "$BACKEND_SERVICE" \
        --region="$REGION" \
        --format="value(status.url)")

    # Cloud Run automatically provides HTTPS for *.run.app domains
    log_success "Backend service deployed: $BACKEND_URL"
    log_info "HTTPS is automatically enabled for Cloud Run services"

    if [ "$FORCE_HTTPS" = "true" ]; then
        log_info "HTTPS enforcement is enabled (FORCE_HTTPS=true)"
    fi
}

# ============================================
# Frontend Services Deployment with HTTPS
# ============================================

deploy_frontend() {
    log_info "Deploying frontend services to Cloud Run..."

    frontend_configs=(
        "client-app:${CLIENT_APP_SERVICE}:Client App for users"
        "admin-app:${ADMIN_APP_SERVICE}:Admin App for administrators"
        "specialist-app:${SPECIALIST_APP_SERVICE}:Specialist App for specialists"
    )

    for config in "${frontend_configs[@]}"; do
        IFS=':' read -r app_dir service_name description <<< "$config"

        log_info "Deploying $service_name..."

        # Determine API base URL
        if [ -n "$BACKEND_DOMAIN" ]; then
            API_BASE="https://${BACKEND_DOMAIN}/api"
        else
            API_BASE="${BACKEND_URL}/api"
        fi

        gcloud run deploy "$service_name" \
            --image="$ARTIFACT_REGISTRY/${service_name}:${ENVIRONMENT}" \
            --region="$REGION" \
            --platform="managed" \
            --allow-unauthenticated \
            --memory="512Mi" \
            --cpu="1" \
            --max-instances=5 \
            --min-instances=1 \
            --concurrency=80 \
            --timeout=60 \
            --set-env-vars="\
                NODE_ENV=production,\
                NUXT_PUBLIC_API_BASE=${API_BASE},\
                NUXT_HOST=0.0.0.0,\
                NUXT_PORT=3000,\
                NUXT_PUBLIC_SITE_URL=https://$(if [ "$app_dir" = "client-app" ] && [ -n "$CLIENT_DOMAIN" ]; then echo "$CLIENT_DOMAIN"; elif [ "$app_dir" = "admin-app" ] && [ -n "$ADMIN_DOMAIN" ]; then echo "$ADMIN_DOMAIN"; elif [ "$app_dir" = "specialist-app" ] && [ -n "$SPECIALIST_DOMAIN" ]; then echo "$SPECIALIST_DOMAIN"; else echo "$service_name"; fi)" \
            --quiet

        SERVICE_URL=$(gcloud run services describe "$service_name" \
            --region="$REGION" \
            --format="value(status.url)")

        log_success "$description deployed: $SERVICE_URL"
        log_info "HTTPS is automatically enabled"
    done
}

# ============================================
# Custom Domain Configuration
# ============================================

setup_custom_domains() {
    if [ "$SKIP_DOMAINS" = "true" ] || [ "$ENABLE_CUSTOM_DOMAINS" = "false" ]; then
        log_info "Skipping custom domain setup"
        return 0
    fi

    log_info "Setting up custom domains..."

    # Map domains to services
    declare -A domain_mappings=(
        ["$BACKEND_DOMAIN"]="$BACKEND_SERVICE"
        ["$CLIENT_DOMAIN"]="$CLIENT_APP_SERVICE"
        ["$ADMIN_DOMAIN"]="$ADMIN_APP_SERVICE"
        ["$SPECIALIST_DOMAIN"]="$SPECIALIST_APP_SERVICE"
    )

    for domain in "${!domain_mappings[@]}"; do
        service="${domain_mappings[$domain]}"

        # Skip if domain is empty
        if [ -z "$domain" ]; then
            continue
        fi

        log_info "Setting up domain $domain for service $service..."

        # Check if domain mapping already exists
        if gcloud run domain-mappings describe --domain="$domain" --region="$REGION" &>/dev/null; then
            log_info "Domain mapping for $domain already exists"
        else
            # Verify domain ownership (if using Google Domains or already verified)
            log_info "Creating domain mapping: $domain → $service"

            # Create domain mapping
            if gcloud run domain-mappings create \
                --service="$service" \
                --domain="$domain" \
                --region="$REGION" \
                --quiet; then
                log_success "Domain mapping created: $domain → $service"

                # Get DNS records to configure
                log_info "Configure your DNS with these records:"
                gcloud run domain-mappings describe --domain="$domain" --region="$REGION" \
                    --format="value(status.resourceRecords)" | tr ';' '\n' | while read record; do
                    if [ -n "$record" ]; then
                        echo "  $record"
                    fi
                done
            else
                log_warning "Failed to create domain mapping for $domain"
                log_warning "You may need to verify domain ownership first:"
                log_warning "  gcloud domains verify $domain"
                log_warning "Or check domain registration and DNS configuration"
            fi
        fi
    done

    # Enable automatic SSL certificates
    log_info "Google-managed SSL certificates will be automatically provisioned"
    log_info "Certificate provisioning may take up to 30 minutes"

    log_success "Custom domain setup complete"
}

# ============================================
# Database Migrations
# ============================================

run_database_migrations() {
    log_info "Running database migrations..."

    # Note: In production, you would use a proper migration strategy
    # This could be a Cloud Run job, Cloud Build step, or manual execution
    log_warning "Database migrations need to be run manually."
    log_warning "Connect to the database and run migrations from the backend service."
    log_warning "Or set up a Cloud Run job for migrations."

    # Provide migration commands
    log_info "Migration options:"
    log_info "1. Cloud Run job: gcloud run jobs create consultant-migrations-${ENVIRONMENT} \\"
    log_info "   --image=$ARTIFACT_REGISTRY/${BACKEND_SERVICE}:${ENVIRONMENT} \\"
    log_info "   --set-env-vars=\"DB_URL=${DATABASE_URL}\" \\"
    log_info "   --command=\"/app/migrate.sh\" \\"
    log_info "   --region=${REGION}"
    log_info ""
    log_info "2. Direct connection: gcloud sql connect ${DATABASE_INSTANCE} --user=postgres"
}

# ============================================
# Output Summary with HTTPS Information
# ============================================

output_summary() {
    log_success "============================================"
    log_success "DEPLOYMENT COMPLETE!"
    log_success "============================================"
    log_success "Environment: $ENVIRONMENT"
    log_success "Project: $GCP_PROJECT_ID"
    log_success "Region: $REGION"
    log_success "HTTPS Enforcement: $FORCE_HTTPS"
    log_success ""

    # Get service URLs
    BACKEND_URL=$(gcloud run services describe "$BACKEND_SERVICE" \
        --region="$REGION" \
        --format="value(status.url)" 2>/dev/null || echo "Not available")

    CLIENT_APP_URL=$(gcloud run services describe "$CLIENT_APP_SERVICE" \
        --region="$REGION" \
        --format="value(status.url)" 2>/dev/null || echo "Not available")

    ADMIN_APP_URL=$(gcloud run services describe "$ADMIN_APP_SERVICE" \
        --region="$REGION" \
        --format="value(status.url)" 2>/dev/null || echo "Not available")

    SPECIALIST_APP_URL=$(gcloud run services describe "$SPECIALIST_APP_SERVICE" \
        --region="$REGION" \
        --format="value(status.url)" 2>/dev/null || echo "Not available")

    echo -e "${GREEN}Service URLs (HTTPS automatically enabled):${NC}"
    echo "Backend API:     $BACKEND_URL"
    echo "Client App:      $CLIENT_APP_URL"
    echo "Admin App:       $ADMIN_APP_URL"
    echo "Specialist App:  $SPECIALIST_APP_URL"
    echo ""

    if [ "$ENABLE_CUSTOM_DOMAINS" = "true" ] && [ "$SKIP_DOMAINS" = "false" ]; then
        echo -e "${GREEN}Custom Domains (SSL certificates auto-provisioned):${NC}"
        [ -n "$BACKEND_DOMAIN" ] && echo "Backend API:     https://$BACKEND_DOMAIN"
        [ -n "$CLIENT_DOMAIN" ] && echo "Client App:      https://$CLIENT_DOMAIN"
        [ -n "$ADMIN_DOMAIN" ] && echo "Admin App:       https://$ADMIN_DOMAIN"
        [ -n "$SPECIALIST_DOMAIN" ] && echo "Specialist App:  https://$SPECIALIST_DOMAIN"
        echo ""
    fi

    echo -e "${GREEN}Database Info:${NC}"
    echo "Instance: $DATABASE_INSTANCE"
    echo "Database: $DATABASE_NAME"
    echo "User:     $DATABASE_USER"
    echo ""

    echo -e "${GREEN}Test Commands:${NC}"
    echo "# Test backend health with HTTPS:"
    echo "curl -k $BACKEND_URL/health"
    echo ""
    echo "# Test API documentation:"
    echo "open $BACKEND_URL/docs"
    echo ""
    echo "# Test SSL certificate (for custom domains):"
    if [ -n "$BACKEND_DOMAIN" ]; then
        echo "openssl s_client -connect $BACKEND_DOMAIN:443 -servername $BACKEND_DOMAIN | openssl x509 -noout -dates"
    fi
    echo ""
    echo "# Connect to database:"
    echo "gcloud sql connect $DATABASE_INSTANCE --user=postgres"
    echo ""

    echo -e "${GREEN}HTTPS Configuration:${NC}"
    echo "✓ All Cloud Run services have automatic HTTPS"
    echo "✓ Google-managed SSL certificates"
    echo "✓ HTTP/2 enabled"
    echo "✓ Security headers configured"
    if [ "$FORCE_HTTPS" = "true" ]; then
        echo "✓ HTTPS enforcement enabled (FORCE_HTTPS=true)"
        echo "✓ Secure cookies enabled"
        echo "✓ Secure sessions enabled"
    fi
    echo ""

    echo -e "${GREEN}Next Steps:${NC}"
    echo "1. Run database migrations"
    echo "2. Configure DNS records for custom domains (if used)"
    echo "3. Set up monitoring and alerting"
    echo "4. Configure CI/CD pipeline"
    echo "5. Test HTTPS endpoints"
    echo ""

    echo -e "${YELLOW}Important Security Notes:${NC}"
    echo "1. Change default passwords in Secret Manager"
    echo "2. Enable IAM authentication for database"
    echo "3. Configure private IP for database"
    echo "4. Set up proper IAM roles and permissions"
    echo "5. Enable Cloud Audit Logging"
    echo "6. Monitor SSL certificate expiration"
    echo ""

    echo -e "${BLUE}HTTPS Verification:${NC}"
    echo "To verify HTTPS is working correctly:"
    echo "1. Check service responds via HTTPS"
    echo "2. Verify SSL certificate is valid"
    echo "3. Test security headers"
    echo "4. Verify HTTP redirects to HTTPS (if FORCE_HTTPS=true)"
}

# ============================================
# Main Deployment Flow
# ============================================

main() {
    echo -e "${BLUE}"
    cat << "EOF"
  ___   ___  ___   _    ___ _   _ _  _  ___ _____ ___
 / __| / __|/ __| /_\  | _ \ | | | \| |/ __|_   _/ _ \
| (__  \__ \ (__ / _ \ |  _/ |_| | .` | (__  | || (_) |
 \___| |___/\___/_/ \_\|_|  \___/|_|\_|\___| |_| \___/

EOF
    echo -e "${NC}"
    echo "GCP Deployment Script for Consultant Platform"
    echo "============================================"
    echo ""

    # Show configuration
    log_info "Configuration:"
    echo "Environment:      $ENVIRONMENT"
    echo "GCP Project:      $GCP_PROJECT_ID"
    echo "Region:           $REGION"
    echo "Backend Service:  $BACKEND_SERVICE"
    echo "Database:         $DATABASE_INSTANCE"
    echo "HTTPS Enforcement: $FORCE_HTTPS"

    if [ "$ENABLE_CUSTOM_DOMAINS" = "true" ] && [ "$SKIP_DOMAINS" = "false" ]; then
        echo "Custom Domains:   Enabled"
        [ -n "$BACKEND_DOMAIN" ] && echo "  Backend:        $BACKEND_DOMAIN"
        [ -n "$CLIENT_DOMAIN" ] && echo "  Client App:     $CLIENT_DOMAIN"
        [ -n "$ADMIN_DOMAIN" ] && echo "  Admin App:      $ADMIN_DOMAIN"
        [ -n "$SPECIALIST_DOMAIN" ] && echo "  Specialist App: $SPECIALIST_DOMAIN"
    else
        echo "Custom Domains:   Disabled"
    fi
    echo ""

    # Confirm deployment
    read -p "Continue with deployment? (y/N): " -r confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        log_info "Deployment cancelled"
        exit 0
    fi

    # Execute deployment steps
    check_prerequisites
    setup_gcp_project
    setup_artifact_registry
    setup_database
    setup_vpc_connector
    setup_secrets
    build_and_push_images
    deploy_backend
    deploy_frontend

    if [ "$SKIP_DOMAINS" = "false" ]; then
        setup_custom_domains
    fi

    run_database_migrations
    output_summary
}

# ============================================
# Script Entry Point
# ============================================

# Run main function
main "$@"
