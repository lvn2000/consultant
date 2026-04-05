#!/bin/bash

# ============================================
# Configuration Module
# ============================================
# Manages configuration for GCP deployment
# Usage: source ./lib/config.sh



# ============================================
# Default Configuration
# ============================================

# Application settings
DEFAULT_APP_NAME="consultant"
DEFAULT_REGION="us-central1"
DEFAULT_ARTIFACT_REGISTRY_REGION="us-central1"

# Environment-specific defaults
declare -A ENV_CONFIGS=(
    [dev:project_suffix]="-dev"
    [dev:region]="us-central1"
    [dev:database_tier]="db-f1-micro"
    [dev:service_account_suffix]="-dev"
    [dev:enable_backup]="false"
    [dev:enable_monitoring]="false"

    [staging:project_suffix]="-staging"
    [staging:region]="us-central1"
    [staging:database_tier]="db-g1-small"
    [staging:service_account_suffix]="-staging"
    [staging:enable_backup]="true"
    [staging:enable_monitoring]="true"

    [prod:project_suffix]=""
    [prod:region]="us-central1"
    [prod:database_tier]="db-custom-2-7680"
    [prod:service_account_suffix]=""
    [prod:enable_backup]="true"
    [prod:enable_monitoring]="true"
)

# Service names
declare -A SERVICE_NAMES=(
    [backend]="consultant-backend"
    [client_app]="consultant-client-app"
    [admin_app]="consultant-admin-app"
    [specialist_app]="consultant-specialist-app"
)

# Database settings
DEFAULT_DATABASE_NAME="consultant"
DEFAULT_DATABASE_USER="consultant_user"
DEFAULT_DATABASE_CONNECTION_LIMIT=5

# Cloud SQL settings
DEFAULT_CLOUDSQL_VERSION="POSTGRES_15"
DEFAULT_CLOUDSQL_AVAILABILITY_TYPE="ZONAL"

# Cloud Run settings
DEFAULT_CLOUD_RUN_MEMORY="2Gi"
DEFAULT_CLOUD_RUN_TIMEOUT="3600"
DEFAULT_CLOUD_RUN_CONCURRENCY="100"
DEFAULT_CLOUD_RUN_MIN_INSTANCES="0"
DEFAULT_CLOUD_RUN_MAX_INSTANCES="100"

# VPC Connector settings
DEFAULT_VPC_CONNECTOR_MACHINE_TYPE="e2-micro"
DEFAULT_VPC_CONNECTOR_MIN_THROUGHPUT="200"
DEFAULT_VPC_CONNECTOR_MAX_THROUGHPUT="300"

# ============================================
# Configuration Variables (can be overridden)
# ============================================

# Core settings
ENVIRONMENT="${ENVIRONMENT:-dev}"
GCP_PROJECT_ID="${GCP_PROJECT_ID:-}"
GCP_BILLING_ACCOUNT="${GCP_BILLING_ACCOUNT:-}"
SCRIPT_DIR="${SCRIPT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
PROJECT_ROOT="${PROJECT_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

# Feature flags
FORCE_HTTPS="${FORCE_HTTPS:-true}"
ENABLE_CUSTOM_DOMAINS="${ENABLE_CUSTOM_DOMAINS:-false}"
SKIP_DOMAINS="${SKIP_DOMAINS:-false}"
SKIP_DATABASE_SETUP="${SKIP_DATABASE_SETUP:-false}"
SKIP_BUILDS="${SKIP_BUILD:-false}"
DRY_RUN="${DRY_RUN:-false}"

# Domain configuration
CUSTOM_DOMAIN="${CUSTOM_DOMAIN:-}"
BACKEND_DOMAIN="${BACKEND_DOMAIN:-}"
CLIENT_DOMAIN="${CLIENT_DOMAIN:-}"
ADMIN_DOMAIN="${ADMIN_DOMAIN:-}"
SPECIALIST_DOMAIN="${SPECIALIST_DOMAIN:-}"

# ============================================
# Configuration Functions
# ============================================

# Get environment-specific configuration value
get_env_config() {
    local key=$1
    local config_key="${ENVIRONMENT}:${key}"
    echo "${ENV_CONFIGS[$config_key]:-}"
}

# Get service name by key
get_service_name() {
    local service_key=$1
    echo "${SERVICE_NAMES[$service_key]:-}"
}

# Validate environment
validate_environment() {
    case "$ENVIRONMENT" in
        dev|staging|prod)
            return 0
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT. Use dev, staging, or prod."
            ;;
    esac
}

# Load configuration from file
load_config_file() {
    local config_file=$1

    if [[ ! -f "$config_file" ]]; then
        log_warning "Configuration file not found: $config_file"
        return 1
    fi

    log_info "Loading configuration from: $config_file"
    source "$config_file"
    return 0
}

# Validate GCP project ID
validate_gcp_project_id() {
    if [[ -z "$GCP_PROJECT_ID" ]]; then
        log_error "GCP_PROJECT_ID is not set"
    fi

    # Validate format: lowercase letters, numbers, and hyphens only
    if ! [[ "$GCP_PROJECT_ID" =~ ^[a-z0-9-]+$ ]]; then
        log_error "Invalid GCP_PROJECT_ID format: $GCP_PROJECT_ID (must contain only lowercase letters, numbers, and hyphens)"
    fi
}

# Get full service name for Cloud Run
get_cloud_run_service_name() {
    local service_key=$1
    local service_name=$(get_service_name "$service_key")

    if [[ -z "$service_name" ]]; then
        log_error "Unknown service: $service_key"
    fi

    echo "$service_name"
}

# Get database instance name
get_database_instance_name() {
    local app_name="${1:-$DEFAULT_APP_NAME}"
    echo "${app_name}-postgres-${ENVIRONMENT}"
}

# Get artifact registry path
get_artifact_registry_path() {
    local image_name=$1
    echo "${DEFAULT_ARTIFACT_REGISTRY_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${DEFAULT_APP_NAME}/${image_name}"
}

# Get VPC connector name
get_vpc_connector_name() {
    echo "${DEFAULT_APP_NAME}-connector-${ENVIRONMENT}"
}

# Display current configuration
print_configuration() {
    log_section "Current Configuration"

    log_variable "Environment" "$ENVIRONMENT"
    log_variable "GCP Project ID" "$GCP_PROJECT_ID"
    log_variable "Region" "$(get_env_config region)"
    log_variable "HTTPS Enforced" "$FORCE_HTTPS"
    log_variable "Custom Domains Enabled" "$ENABLE_CUSTOM_DOMAINS"
    log_variable "Dry Run Mode" "$DRY_RUN"

    if [[ "$ENABLE_CUSTOM_DOMAINS" == "true" ]]; then
        echo ""
        log_info "Custom Domains Configuration:"
        [[ -n "$BACKEND_DOMAIN" ]] && log_variable "Backend Domain" "$BACKEND_DOMAIN"
        [[ -n "$CLIENT_DOMAIN" ]] && log_variable "Client Domain" "$CLIENT_DOMAIN"
        [[ -n "$ADMIN_DOMAIN" ]] && log_variable "Admin Domain" "$ADMIN_DOMAIN"
        [[ -n "$SPECIALIST_DOMAIN" ]] && log_variable "Specialist Domain" "$SPECIALIST_DOMAIN"
    fi
}

# Initialize configuration
init_config() {
    validate_environment
    validate_gcp_project_id
    print_configuration
}
