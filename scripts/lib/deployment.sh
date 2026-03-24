#!/bin/bash

# ============================================
# Deployment Module
# ============================================
# Orchestrates the deployment of Consultant Platform to GCP
# Usage: source ./lib/deployment.sh

# Source required modules
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/logging.sh"
source "${SCRIPT_DIR}/config.sh"
source "${SCRIPT_DIR}/gcp.sh"

# ============================================
# Prerequisites Check
# ============================================

check_prerequisites() {
    log_section "Checking Prerequisites"

    local missing_tools=()

    # Check required tools
    local required_tools=(
        "gcloud"
        "docker"
        "git"
        "kubectl"
    )

    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        else
            log_success "✓ $tool is installed"
        fi
    done

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
    fi

    # Check gcloud authentication
    check_gcloud_auth

    # Check project access
    if ! gcloud projects describe "$GCP_PROJECT_ID" &> /dev/null; then
        log_error "Cannot access GCP project: $GCP_PROJECT_ID"
    fi

    log_success "All prerequisites met"
}

# ============================================
# Build & Push Docker Images
# ============================================

build_backend_image() {
    local image_name=$1
    local dockerfile_path=${2:-"Dockerfile"}
    local build_context=${3:-"."}

    if [[ -z "$image_name" ]]; then
        log_error "Image name is required"
    fi

    log_step "1.1" "Building backend Docker image: $image_name"

    if [[ ! -f "$dockerfile_path" ]]; then
        log_error "Dockerfile not found: $dockerfile_path"
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would build image: docker build -t $image_name -f $dockerfile_path $build_context"
        return 0
    fi

    if ! docker build -t "$image_name" -f "$dockerfile_path" "$build_context" &> /dev/null; then
        log_error "Failed to build Docker image: $image_name"
    fi

    log_success "Backend Docker image built: $image_name"
}

build_frontend_images() {
    local apps=("admin-app" "specialist-app" "client-app")
    local dockerfile_path="${1:-frontend.Dockerfile}"

    log_step "1.2" "Building frontend Docker images"

    for app in "${apps[@]}"; do
        local image_name="${DEFAULT_APP_NAME}-${app}"
        log_info "  Building: $image_name"

        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would build image: $image_name"
            continue
        fi

        if ! docker build -t "$image_name" -f "$dockerfile_path" \
            --build-arg "APP_NAME=$app" . &> /dev/null; then
            log_error "Failed to build Docker image: $image_name"
        fi

        log_success "  ✓ $image_name"
    done

    log_success "All frontend images built successfully"
}

push_images_to_registry() {
    log_step "1.3" "Pushing Docker images to Artifact Registry"

    local services=("backend" "admin-app" "specialist-app" "client-app")

    for service in "${services[@]}"; do
        local image_name="${DEFAULT_APP_NAME}-${service}"
        local registry_image=$(get_artifact_registry_path "$image_name")

        log_info "  Pushing: $image_name"

        push_docker_image "$image_name" "$registry_image"
    done

    log_success "All images pushed to Artifact Registry"
}

# ============================================
# Database Setup & Migrations
# ============================================

setup_database() {
    log_section "Setting Up Database"

    if [[ "$SKIP_DATABASE_SETUP" == "true" ]]; then
        log_info "Skipping database setup (SKIP_DATABASE_SETUP=true)"
        return 0
    fi

    local instance_name=$(get_database_instance_name)
    local database_tier=$(get_env_config database_tier)
    local region=$(get_env_config region)

    log_step "2.1" "Creating Cloud SQL instance: $instance_name"
    create_cloud_sql_instance "$instance_name" "$database_tier" "$region"

    log_step "2.2" "Creating database: $DEFAULT_DATABASE_NAME"
    create_cloud_sql_database "$instance_name" "$DEFAULT_DATABASE_NAME"

    log_step "2.3" "Creating database user: $DEFAULT_DATABASE_USER"
    local db_password=$(generate_secure_password)
    create_cloud_sql_user "$instance_name" "$DEFAULT_DATABASE_USER" "$db_password"

    # Store password securely
    store_database_password "$db_password"

    log_success "Database setup completed"
}

generate_secure_password() {
    local length=${1:-32}
    openssl rand -base64 "$length" | tr -d "=+/" | cut -c1-"$length"
}

store_database_password() {
    local password=$1
    # This should store the password securely in GCP Secret Manager
    log_info "Storing database password in Secret Manager"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would store password in GCP Secret Manager"
        return 0
    fi

    echo -n "$password" | gcloud secrets create "${DEFAULT_APP_NAME}-db-password" \
        --data-file=- \
        --replication-policy="automatic" \
        --quiet 2>/dev/null || log_warning "Secret already exists or could not be created"
}

run_database_migrations() {
    log_section "Running Database Migrations"

    local instance_name=$(get_database_instance_name)
    local db_connection_string="postgresql://${DEFAULT_DATABASE_USER}@/${DEFAULT_DATABASE_NAME}?host=/cloudsql/${GCP_PROJECT_ID}:us-central1:${instance_name}"

    log_step "3.1" "Running Flyway migrations"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would run database migrations"
        return 0
    fi

    # This would typically run migrations using Flyway or Liquibase
    log_info "Connection string: $db_connection_string"
    log_warning "Database migrations should be run manually or through CI/CD pipeline"

    log_success "Migration setup completed"
}

# ============================================
# Service Deployment
# ============================================

deploy_backend_service() {
    log_section "Deploying Backend Service"

    local service_name=$(get_service_name "backend")
    local image=$(get_artifact_registry_path "${DEFAULT_APP_NAME}-backend")
    local region=$(get_env_config region)
    local memory="${BACKEND_MEMORY:-2Gi}"

    log_step "4.1" "Deploying backend service: $service_name"

    deploy_cloud_run_service "$service_name" "$image" "$region" "$memory"

    log_step "4.2" "Configuring backend environment variables"
    local db_instance=$(get_database_instance_name)
    local env_vars=(
        "ENVIRONMENT=$ENVIRONMENT"
        "DATABASE_HOST=/cloudsql/${GCP_PROJECT_ID}:us-central1:${db_instance}"
        "DATABASE_NAME=$DEFAULT_DATABASE_NAME"
        "DATABASE_USER=$DEFAULT_DATABASE_USER"
        "PORT=8080"
    )

    set_cloud_run_env_vars "$service_name" "$region" "${env_vars[@]}"

    log_step "4.3" "Setting up custom domain for backend"
    if [[ -n "$BACKEND_DOMAIN" && "$SKIP_DOMAINS" != "true" ]]; then
        map_custom_domain "$service_name" "$BACKEND_DOMAIN" "$region"
    fi

    local backend_url=$(get_service_url "$service_name" "$region")
    log_success "Backend service deployed: $backend_url"
}

deploy_frontend_services() {
    log_section "Deploying Frontend Services"

    local services=("admin-app" "specialist-app" "client-app")
    local domains=("$ADMIN_DOMAIN" "$SPECIALIST_DOMAIN" "$CLIENT_DOMAIN")
    local region=$(get_env_config region)
    local memory="${FRONTEND_MEMORY:-1Gi}"

    for i in "${!services[@]}"; do
        local app="${services[$i]}"
        local domain="${domains[$i]}"
        local service_name=$(get_service_name "${app/-/_}")
        local image=$(get_artifact_registry_path "${DEFAULT_APP_NAME}-${app}")

        log_step "5.$((i+1))" "Deploying frontend service: $service_name"

        deploy_cloud_run_service "$service_name" "$image" "$region" "$memory"

        if [[ -n "$domain" && "$SKIP_DOMAINS" != "true" ]]; then
            log_info "  Mapping custom domain: $domain"
            map_custom_domain "$service_name" "$domain" "$region"
        fi

        local service_url=$(get_service_url "$service_name" "$region")
        log_success "Frontend service deployed: $app -> $service_url"
    done

    log_success "All frontend services deployed successfully"
}

# ============================================
# VPC & Networking Setup
# ============================================

setup_networking() {
    log_section "Setting Up Networking"

    local connector_name=$(get_vpc_connector_name)
    local region=$(get_env_config region)

    log_step "6.1" "Creating VPC connector: $connector_name"
    create_vpc_connector "$connector_name" "default" "default" "$region"

    log_success "Networking setup completed"
}

# ============================================
# Deployment Summary
# ============================================

display_deployment_summary() {
    log_section "Deployment Summary"

    local environment_config=$(get_env_config enable_monitoring)

    echo ""
    log_info "Deployment Configuration:"
    log_variable "Environment" "$ENVIRONMENT"
    log_variable "GCP Project" "$GCP_PROJECT_ID"
    log_variable "Region" "$(get_env_config region)"
    log_variable "Database Tier" "$(get_env_config database_tier)"
    log_variable "Monitoring Enabled" "$environment_config"

    echo ""
    log_info "Deployed Services:"

    local backend_url=$(get_service_url "$(get_service_name backend)" "$(get_env_config region)" 2>/dev/null)
    [[ -n "$backend_url" ]] && log_variable "Backend API" "$backend_url"

    local admin_url=$(get_service_url "$(get_service_name admin_app)" "$(get_env_config region)" 2>/dev/null)
    [[ -n "$admin_url" ]] && log_variable "Admin App" "$admin_url"

    local client_url=$(get_service_url "$(get_service_name client_app)" "$(get_env_config region)" 2>/dev/null)
    [[ -n "$client_url" ]] && log_variable "Client App" "$client_url"

    echo ""
    log_success "Deployment completed successfully!"
}

# ============================================
# Main Deployment Function
# ============================================

execute_deployment() {
    log_section "Starting Consultant Platform Deployment"

    log_info "Environment: $ENVIRONMENT"
    log_info "Project ID: $GCP_PROJECT_ID"
    log_info "Dry Run Mode: $DRY_RUN"

    # Initialize project
    set_gcp_project "$GCP_PROJECT_ID"
    enable_gcp_apis

    # Check prerequisites
    check_prerequisites

    # Build and push images
    if [[ "$SKIP_BUILDS" != "true" ]]; then
        build_backend_image "${DEFAULT_APP_NAME}-backend" "Dockerfile"
        build_frontend_images "frontend.Dockerfile"
        push_images_to_registry
    fi

    # Setup database
    setup_database

    # Setup networking
    setup_networking

    # Deploy services
    deploy_backend_service
    deploy_frontend_services

    # Run migrations
    run_database_migrations

    # Display summary
    display_deployment_summary
}

# Export functions for use in main script
export -f check_prerequisites
export -f build_backend_image
export -f build_frontend_images
export -f push_images_to_registry
export -f setup_database
export -f setup_networking
export -f deploy_backend_service
export -f deploy_frontend_services
export -f execute_deployment
export -f display_deployment_summary
