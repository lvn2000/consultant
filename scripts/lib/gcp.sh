#!/bin/bash

# ============================================
# GCP Utilities Module
# ============================================
# Provides utility functions for interacting with Google Cloud Platform
# Usage: source ./lib/gcp.sh

# Note: This module should be sourced after logging.sh and config.sh
# Source order in main script: logging.sh -> config.sh -> gcp.sh -> deployment.sh

# ============================================
# GCP Authentication & Project Setup
# ============================================

# Check if gcloud is installed and authenticated
check_gcloud_auth() {
    log_info "Checking gcloud authentication..."

    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI is not installed. Please install Google Cloud SDK."
    fi

    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        log_error "No active gcloud authentication found. Run 'gcloud auth login'"
    fi

    log_success "gcloud CLI is authenticated"
}

# Set active GCP project
set_gcp_project() {
    local project_id=$1

    if [[ -z "$project_id" ]]; then
        log_error "Project ID is required"
    fi

    log_info "Setting active GCP project to: $project_id"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would execute: gcloud config set project $project_id"
        return 0
    fi

    if ! gcloud config set project "$project_id" &> /dev/null; then
        log_error "Failed to set GCP project: $project_id"
    fi

    log_success "GCP project set to: $project_id"
}

# Get current active project
get_active_project() {
    gcloud config get-value project 2>/dev/null || echo ""
}

# Enable required APIs
enable_gcp_apis() {
    local apis=(
        "compute.googleapis.com"
        "run.googleapis.com"
        "sqladmin.googleapis.com"
        "artifactregistry.googleapis.com"
        "servicenetworking.googleapis.com"
        "cloudresourcemanager.googleapis.com"
        "iam.googleapis.com"
        "containerregistry.googleapis.com"
    )

    log_info "Enabling required GCP APIs..."

    for api in "${apis[@]}"; do
        log_info "Enabling API: $api"

        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would execute: gcloud services enable $api"
            continue
        fi

        if ! gcloud services enable "$api" --quiet 2>&1 | grep -q "Operation completed"; then
            log_warning "API $api might already be enabled or failed to enable"
        fi
    done

    log_success "GCP APIs enabled successfully"
}

# ============================================
# Service Account Management
# ============================================

# Create service account
create_service_account() {
    local account_name=$1
    local display_name=${2:-"Service Account"}

    if [[ -z "$account_name" ]]; then
        log_error "Service account name is required"
    fi

    local full_account="${account_name}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

    log_info "Creating service account: $account_name"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create service account: $full_account"
        return 0
    fi

    if gcloud iam service-accounts describe "$full_account" &> /dev/null; then
        log_warning "Service account already exists: $full_account"
        return 0
    fi

    if ! gcloud iam service-accounts create "$account_name" \
        --display-name="$display_name" \
        --quiet 2>&1 | grep -q "created"; then
        log_error "Failed to create service account: $account_name"
    fi

    log_success "Service account created: $full_account"
}

# Grant IAM role to service account
grant_iam_role() {
    local service_account=$1
    local role=$2
    local member="serviceAccount:${service_account}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

    if [[ -z "$service_account" ]] || [[ -z "$role" ]]; then
        log_error "Service account and role are required"
    fi

    log_info "Granting role $role to $member"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would grant role: gcloud projects add-iam-policy-binding $GCP_PROJECT_ID --member=$member --role=$role"
        return 0
    fi

    if ! gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
        --member="$member" \
        --role="$role" \
        --quiet &> /dev/null; then
        log_error "Failed to grant role $role to $member"
    fi

    log_success "Role granted: $role -> $member"
}

# ============================================
# Artifact Registry Operations
# ============================================

# Create artifact registry repository
create_artifact_registry() {
    local repo_name=$1
    local region=${2:-$(get_env_config region)}

    if [[ -z "$repo_name" ]] || [[ -z "$region" ]]; then
        log_error "Repository name and region are required"
    fi

    log_info "Creating artifact registry repository: $repo_name in $region"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create artifact registry: $repo_name"
        return 0
    fi

    if gcloud artifacts repositories describe "$repo_name" --location="$region" &> /dev/null; then
        log_warning "Repository already exists: $repo_name"
        return 0
    fi

    if ! gcloud artifacts repositories create "$repo_name" \
        --repository-format="docker" \
        --location="$region" \
        --quiet &> /dev/null; then
        log_error "Failed to create artifact registry: $repo_name"
    fi

    log_success "Artifact registry created: $repo_name"
}

# Push Docker image to artifact registry
push_docker_image() {
    local local_image=$1
    local registry_image=$2

    if [[ -z "$local_image" ]] || [[ -z "$registry_image" ]]; then
        log_error "Local image and registry image are required"
    fi

    log_info "Pushing Docker image: $local_image -> $registry_image"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would push image: docker tag $local_image $registry_image && docker push $registry_image"
        return 0
    fi

    if ! docker tag "$local_image" "$registry_image"; then
        log_error "Failed to tag Docker image: $local_image"
    fi

    if ! docker push "$registry_image"; then
        log_error "Failed to push Docker image: $registry_image"
    fi

    log_success "Docker image pushed successfully: $registry_image"
}

# ============================================
# Cloud SQL Operations
# ============================================

# Create Cloud SQL instance
create_cloud_sql_instance() {
    local instance_name=$1
    local database_tier=${2:-$(get_env_config database_tier)}
    local region=${3:-$(get_env_config region)}

    if [[ -z "$instance_name" ]]; then
        log_error "Instance name is required"
    fi

    log_info "Creating Cloud SQL instance: $instance_name"
    log_debug "  Tier: $database_tier"
    log_debug "  Region: $region"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create Cloud SQL instance: $instance_name"
        return 0
    fi

    if gcloud sql instances describe "$instance_name" &> /dev/null; then
        log_warning "Cloud SQL instance already exists: $instance_name"
        return 0
    fi

    if ! gcloud sql instances create "$instance_name" \
        --database-version="POSTGRES_15" \
        --tier="$database_tier" \
        --region="$region" \
        --availability-type="ZONAL" \
        --enable-bin-log \
        --backup-start-time="02:00" \
        --quiet &> /dev/null; then
        log_error "Failed to create Cloud SQL instance: $instance_name"
    fi

    log_success "Cloud SQL instance created: $instance_name"
}

# Create database in Cloud SQL instance
create_cloud_sql_database() {
    local instance_name=$1
    local database_name=${2:-$DEFAULT_DATABASE_NAME}

    if [[ -z "$instance_name" ]]; then
        log_error "Instance name is required"
    fi

    log_info "Creating database: $database_name in instance: $instance_name"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create database: $database_name"
        return 0
    fi

    if ! gcloud sql databases create "$database_name" \
        --instance="$instance_name" \
        --quiet &> /dev/null; then
        log_error "Failed to create database: $database_name"
    fi

    log_success "Database created: $database_name"
}

# Create Cloud SQL user
create_cloud_sql_user() {
    local instance_name=$1
    local username=${2:-$DEFAULT_DATABASE_USER}
    local password=$3

    if [[ -z "$instance_name" ]] || [[ -z "$password" ]]; then
        log_error "Instance name and password are required"
    fi

    log_info "Creating Cloud SQL user: $username"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create user: $username"
        return 0
    fi

    if ! gcloud sql users create "$username" \
        --instance="$instance_name" \
        --password="$password" \
        --quiet &> /dev/null; then
        log_error "Failed to create Cloud SQL user: $username"
    fi

    log_success "Cloud SQL user created: $username"
}

# ============================================
# Cloud Run Operations
# ============================================

# Deploy service to Cloud Run
deploy_cloud_run_service() {
    local service_name=$1
    local image=$2
    local region=${3:-$(get_env_config region)}
    local memory=${4:-$DEFAULT_CLOUD_RUN_MEMORY}
    local timeout=${5:-$DEFAULT_CLOUD_RUN_TIMEOUT}

    if [[ -z "$service_name" ]] || [[ -z "$image" ]]; then
        log_error "Service name and image are required"
    fi

    log_info "Deploying Cloud Run service: $service_name"
    log_debug "  Image: $image"
    log_debug "  Region: $region"
    log_debug "  Memory: $memory"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would deploy Cloud Run service: $service_name"
        return 0
    fi

    if ! gcloud run deploy "$service_name" \
        --image="$image" \
        --region="$region" \
        --memory="$memory" \
        --timeout="$timeout" \
        --allow-unauthenticated \
        --platform="managed" \
        --quiet &> /dev/null; then
        log_error "Failed to deploy Cloud Run service: $service_name"
    fi

    log_success "Cloud Run service deployed: $service_name"
}

# Update Cloud Run service environment variables
set_cloud_run_env_vars() {
    local service_name=$1
    local region=${2:-$(get_env_config region)}
    shift 2
    local env_vars=("$@")

    if [[ -z "$service_name" ]]; then
        log_error "Service name is required"
    fi

    log_info "Setting environment variables for Cloud Run service: $service_name"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would set environment variables for: $service_name"
        return 0
    fi

    local env_str=""
    for var in "${env_vars[@]}"; do
        env_str="${env_str},${var}"
    done
    env_str="${env_str:1}"  # Remove leading comma

    if ! gcloud run services update "$service_name" \
        --region="$region" \
        --update-env-vars="$env_str" \
        --quiet &> /dev/null; then
        log_error "Failed to set environment variables for: $service_name"
    fi

    log_success "Environment variables set for: $service_name"
}

# ============================================
# VPC Connector Operations
# ============================================

# Create VPC connector
create_vpc_connector() {
    local connector_name=$1
    local network=${2:-"default"}
    local subnet=${3:-"default"}
    local region=${4:-$(get_env_config region)}
    local machine_type=${5:-$DEFAULT_VPC_CONNECTOR_MACHINE_TYPE}

    if [[ -z "$connector_name" ]]; then
        log_error "Connector name is required"
    fi

    log_info "Creating VPC connector: $connector_name"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create VPC connector: $connector_name"
        return 0
    fi

    if gcloud compute networks vpc-tunnels describe "$connector_name" --region="$region" &> /dev/null; then
        log_warning "VPC connector already exists: $connector_name"
        return 0
    fi

    if ! gcloud compute networks vpc-tunnels create "$connector_name" \
        --network="$network" \
        --subnet="$subnet" \
        --region="$region" \
        --machine-type="$machine_type" \
        --min-throughput="$DEFAULT_VPC_CONNECTOR_MIN_THROUGHPUT" \
        --max-throughput="$DEFAULT_VPC_CONNECTOR_MAX_THROUGHPUT" \
        --quiet &> /dev/null; then
        log_error "Failed to create VPC connector: $connector_name"
    fi

    log_success "VPC connector created: $connector_name"
}

# ============================================
# Domain & SSL/TLS Operations
# ============================================

# Map custom domain to Cloud Run service
map_custom_domain() {
    local service_name=$1
    local domain=$2
    local region=${3:-$(get_env_config region)}

    if [[ -z "$service_name" ]] || [[ -z "$domain" ]]; then
        log_error "Service name and domain are required"
    fi

    log_info "Mapping custom domain: $domain -> $service_name"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would map domain: $domain to service: $service_name"
        return 0
    fi

    if ! gcloud run domain-mappings create --service="$service_name" \
        --domain="$domain" \
        --region="$region" \
        --quiet 2>&1 | grep -q "Successfully created"; then
        log_warning "Failed to map domain or domain already mapped: $domain"
    fi

    log_success "Custom domain mapped: $domain"
}

# ============================================
# Helper Functions
# ============================================

# Wait for operation to complete
wait_for_operation() {
    local operation_name=$1
    local timeout=${2:-300}

    log_info "Waiting for operation: $operation_name"

    local elapsed=0
    while [[ $elapsed -lt $timeout ]]; do
        if gcloud compute operations describe "$operation_name" --global &> /dev/null; then
            local status=$(gcloud compute operations describe "$operation_name" --global --format="value(status)")
            if [[ "$status" == "DONE" ]]; then
                log_success "Operation completed: $operation_name"
                return 0
            fi
        fi
        sleep 5
        elapsed=$((elapsed + 5))
    done

    log_error "Operation timed out: $operation_name"
}

# Get service URL
get_service_url() {
    local service_name=$1
    local region=${2:-$(get_env_config region)}

    if [[ -z "$service_name" ]]; then
        log_error "Service name is required"
    fi

    gcloud run services describe "$service_name" --region="$region" --format="value(status.url)" 2>/dev/null || echo ""
}

# Display resource summary
display_gcp_resources() {
    log_section "Deployed GCP Resources"

    log_info "Cloud Run Services:"
    gcloud run services list --format="table(metadata.name, status.url)" 2>/dev/null || log_warning "No services found"

    log_info ""
    log_info "Cloud SQL Instances:"
    gcloud sql instances list --format="table(name, state, databaseVersion)" 2>/dev/null || log_warning "No instances found"
}
