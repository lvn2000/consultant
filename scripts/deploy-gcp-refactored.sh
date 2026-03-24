#!/bin/bash

# ============================================
# GCP Deployment Script for Consultant Platform (Refactored)
# ============================================
# Main deployment orchestration script
# This script sources modular components for a clean, maintainable deployment process
#
# Usage: ./deploy-gcp-refactored.sh [environment] [options]
#   environment: dev, staging, prod (default: dev)
#   options:
#     --domain=DOMAIN                Custom domain for all services
#     --backend-domain=DOMAIN        Custom domain for backend service
#     --client-domain=DOMAIN         Custom domain for client app
#     --admin-domain=DOMAIN          Custom domain for admin app
#     --specialist-domain=DOMAIN     Custom domain for specialist app
#     --no-https                     Disable HTTPS enforcement (not recommended)
#     --skip-domains                 Skip custom domain setup
#     --skip-builds                  Skip Docker image builds
#     --skip-database                Skip database setup
#     --dry-run                      Simulate deployment without making changes
#     -h, --help                     Show this help message
#
# Environment Variables:
#   GCP_PROJECT_ID                 GCP Project ID (required)
#   GCP_BILLING_ACCOUNT            Billing account ID (optional)
#   LOG_LEVEL                      Logging level (DEBUG, INFO, WARNING, ERROR)
#   LOG_WITH_TIMESTAMPS            Enable timestamps in logs (true/false)
#   ENVIRONMENT                    Deployment environment (dev, staging, prod)
#   FORCE_HTTPS                    Enforce HTTPS (true/false)
#   DRY_RUN                        Simulate deployment (true/false)
#
# ============================================

set -euo pipefail

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
ENVIRONMENT="${ENVIRONMENT:-dev}"
DRY_RUN="${DRY_RUN:-false}"
FORCE_HTTPS="${FORCE_HTTPS:-true}"
ENABLE_CUSTOM_DOMAINS="${ENABLE_CUSTOM_DOMAINS:-false}"
SKIP_DOMAINS="${SKIP_DOMAINS:-false}"
SKIP_DATABASE_SETUP="${SKIP_DATABASE_SETUP:-false}"
SKIP_BUILDS="${SKIP_BUILD:-false}"

# Logging defaults
LOG_LEVEL="${LOG_LEVEL:-1}"  # INFO level
LOG_WITH_TIMESTAMPS="${LOG_WITH_TIMESTAMPS:-false}"

# Domain configuration
CUSTOM_DOMAIN=""
BACKEND_DOMAIN=""
CLIENT_DOMAIN=""
ADMIN_DOMAIN=""
SPECIALIST_DOMAIN=""

# ============================================
# Source Library Modules
# ============================================

# Check if lib directory exists
if [[ ! -d "$SCRIPT_DIR/lib" ]]; then
    echo "ERROR: Library modules not found at $SCRIPT_DIR/lib"
    echo "Please ensure the refactored deployment scripts are properly installed."
    exit 1
fi

# Source modules
source "$SCRIPT_DIR/lib/logging.sh"
source "$SCRIPT_DIR/lib/config.sh"
source "$SCRIPT_DIR/lib/gcp.sh"
source "$SCRIPT_DIR/lib/deployment.sh"

# ============================================
# Command Line Argument Parsing
# ============================================

parse_arguments() {
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
            --skip-builds)
                SKIP_BUILDS="true"
                shift
                ;;
            --skip-database)
                SKIP_DATABASE_SETUP="true"
                shift
                ;;
            --dry-run)
                DRY_RUN="true"
                shift
                ;;
            --log-level=*)
                LOG_LEVEL="${1#*=}"
                shift
                ;;
            --with-timestamps)
                LOG_WITH_TIMESTAMPS="true"
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown argument: $1"
                ;;
        esac
    done
}

# ============================================
# Help Message
# ============================================

show_help() {
    cat << EOF
Usage: $(basename "$0") [environment] [options]

ENVIRONMENTS:
  dev              Development environment (default)
  staging          Staging environment
  prod             Production environment

OPTIONS:
  --domain=DOMAIN                 Custom domain for all services
                                 (e.g., --domain=myapp.com)
  --backend-domain=DOMAIN         Custom domain for backend API
                                 (e.g., --backend-domain=api.myapp.com)
  --client-domain=DOMAIN          Custom domain for client app
                                 (e.g., --client-domain=app.myapp.com)
  --admin-domain=DOMAIN           Custom domain for admin app
                                 (e.g., --admin-domain=admin.myapp.com)
  --specialist-domain=DOMAIN      Custom domain for specialist app
                                 (e.g., --specialist-domain=specialist.myapp.com)
  --no-https                      Disable HTTPS enforcement (NOT RECOMMENDED)
  --skip-domains                  Skip custom domain configuration
  --skip-builds                   Skip Docker image builds
  --skip-database                 Skip database setup
  --dry-run                       Simulate deployment without making changes
  --log-level=LEVEL              Set logging level (DEBUG, INFO, WARNING, ERROR)
  --with-timestamps              Enable timestamps in log output
  -h, --help                     Show this help message

ENVIRONMENT VARIABLES:
  GCP_PROJECT_ID                GCP Project ID (required if not provided interactively)
  GCP_BILLING_ACCOUNT           Billing account ID (optional)
  ENVIRONMENT                   Deployment environment (overrides command line)
  FORCE_HTTPS                   Enforce HTTPS (true/false)
  DRY_RUN                       Simulate deployment (true/false)
  LOG_LEVEL                     Logging level (0=DEBUG, 1=INFO, 2=WARNING, 3=ERROR)
  LOG_WITH_TIMESTAMPS           Enable timestamps (true/false)

EXAMPLES:

  # Deploy to development environment
  $0 dev

  # Deploy to staging with custom domain
  $0 staging --domain=staging.myapp.com

  # Deploy to production with individual service domains
  $0 prod \\
    --backend-domain=api.myapp.com \\
    --client-domain=app.myapp.com \\
    --admin-domain=admin.myapp.com

  # Dry run to see what would be deployed
  $0 prod --dry-run

  # Deploy with debug logging
  $0 dev --log-level=DEBUG --with-timestamps

EOF
}

# ============================================
# Interactive Project ID Prompt
# ============================================

prompt_for_gcp_project_id() {
    if [[ -n "${GCP_PROJECT_ID:-}" ]]; then
        return 0
    fi

    log_section "GCP Project Setup"

    echo "No GCP_PROJECT_ID found in environment."
    echo ""
    echo "Options:"
    echo "  1) Use an existing GCP project"
    echo "  2) Create a new project"
    echo ""

    read -p "Select option (1 or 2): " option

    case $option in
        1)
            read -p "Enter your existing GCP Project ID: " GCP_PROJECT_ID
            ;;
        2)
            read -p "Enter desired project name (will be suffixed with environment): " project_name
            GCP_PROJECT_ID="${project_name}$(get_env_config project_suffix)"
            ;;
        *)
            log_error "Invalid option selected"
            ;;
    esac

    if [[ -z "$GCP_PROJECT_ID" ]]; then
        log_error "GCP Project ID cannot be empty"
    fi

    log_success "Using GCP Project ID: $GCP_PROJECT_ID"
}

# ============================================
# Deployment Validation
# ============================================

validate_deployment_config() {
    log_section "Validating Deployment Configuration"

    # Validate environment
    case "$ENVIRONMENT" in
        dev|staging|prod)
            log_success "Environment validation passed: $ENVIRONMENT"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT. Use dev, staging, or prod."
            ;;
    esac

    # Validate GCP Project ID
    if [[ -z "$GCP_PROJECT_ID" ]]; then
        log_error "GCP_PROJECT_ID is not set"
    fi

    if ! [[ "$GCP_PROJECT_ID" =~ ^[a-z0-9-]+$ ]]; then
        log_error "Invalid GCP_PROJECT_ID format: $GCP_PROJECT_ID"
    fi

    log_success "GCP Project ID validation passed: $GCP_PROJECT_ID"

    # Validate HTTPS settings
    if [[ "$FORCE_HTTPS" != "true" && "$FORCE_HTTPS" != "false" ]]; then
        log_error "Invalid FORCE_HTTPS value: $FORCE_HTTPS (must be true or false)"
    fi

    log_success "All validation checks passed"
}

# ============================================
# Main Deployment Flow
# ============================================

main() {
    # Parse command line arguments
    parse_arguments "$@"

    # Initialize logging
    export LOG_LEVEL LOG_WITH_TIMESTAMPS

    log_section "Consultant Platform - GCP Deployment"
    log_info "Version: 2.0 (Refactored)"
    log_info "Script Directory: $SCRIPT_DIR"
    log_info "Project Root: $PROJECT_ROOT"

    # Prompt for GCP project if needed
    prompt_for_gcp_project_id

    # Export configuration variables for sourced modules
    export ENVIRONMENT GCP_PROJECT_ID DRY_RUN FORCE_HTTPS
    export ENABLE_CUSTOM_DOMAINS SKIP_DOMAINS SKIP_DATABASE_SETUP SKIP_BUILDS
    export CUSTOM_DOMAIN BACKEND_DOMAIN CLIENT_DOMAIN ADMIN_DOMAIN SPECIALIST_DOMAIN
    export SCRIPT_DIR PROJECT_ROOT

    # Validate deployment configuration
    validate_deployment_config

    # Set default domains if custom domain provided
    if [[ -n "$CUSTOM_DOMAIN" ]]; then
        [[ -z "$BACKEND_DOMAIN" ]] && BACKEND_DOMAIN="api.${CUSTOM_DOMAIN}"
        [[ -z "$CLIENT_DOMAIN" ]] && CLIENT_DOMAIN="app.${CUSTOM_DOMAIN}"
        [[ -z "$ADMIN_DOMAIN" ]] && ADMIN_DOMAIN="admin.${CUSTOM_DOMAIN}"
        [[ -z "$SPECIALIST_DOMAIN" ]] && SPECIALIST_DOMAIN="specialist.${CUSTOM_DOMAIN}"
    fi

    # Display configuration
    init_config

    # Display warning for production
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        log_section "PRODUCTION DEPLOYMENT WARNING"
        log_warning "You are about to deploy to PRODUCTION"
        log_warning "This will affect live services and customer data"
        echo ""
        read -p "Are you sure you want to proceed? (type 'yes' to confirm): " confirmation
        if [[ "$confirmation" != "yes" ]]; then
            log_info "Deployment cancelled"
            exit 0
        fi
    fi

    # Display dry-run notice
    if [[ "$DRY_RUN" == "true" ]]; then
        log_section "DRY RUN MODE"
        log_warning "Running in DRY RUN mode - no changes will be made to GCP resources"
        echo ""
    fi

    # Execute deployment
    execute_deployment

    # Success message
    log_section "Deployment Completed"
    log_success "Consultant Platform has been successfully deployed to $ENVIRONMENT"
    log_info "For more information, run: $0 --help"
}

# ============================================
# Error Handling
# ============================================

trap 'log_error_continue "Script interrupted"; exit 130' INT TERM

# ============================================
# Entry Point
# ============================================

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
