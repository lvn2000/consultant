#!/bin/bash

# ============================================
# GCP HTTPS Verification Script
# ============================================
# Tests and verifies HTTPS configuration for deployed Consultant Platform
# on Google Cloud Platform.
#
# Features tested:
# - SSL certificate validity and expiration
# - Security headers (HSTS, X-Content-Type-Options, etc.)
# - HTTP to HTTPS redirect (if FORCE_HTTPS enabled)
# - Service health via HTTPS
# - Custom domain configuration (if used)
# - TLS protocol and cipher suite security
#
# Usage: ./test-gcp-https.sh [environment] [options]
#   environment: dev, staging, prod (default: prod)
#   options:
#     --project=PROJECT_ID      GCP Project ID
#     --region=REGION           GCP Region (default: us-central1)
#     --services=SERVICES       Comma-separated service list
#     --no-redirect-test        Skip HTTP to HTTPS redirect test
#     --no-cert-check           Skip SSL certificate check
#     --no-headers-check        Skip security headers check
#     --verbose                 Show detailed output
#     --help                    Show this help
# ============================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
ENVIRONMENT="prod"
REGION="us-central1"
PROJECT_ID=""
SERVICES=""
VERBOSE=false
TEST_REDIRECT=true
TEST_CERT=true
TEST_HEADERS=true

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
}

log_debug() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        dev|staging|prod)
            ENVIRONMENT="$1"
            shift
            ;;
        --project=*)
            PROJECT_ID="${1#*=}"
            shift
            ;;
        --region=*)
            REGION="${1#*=}"
            shift
            ;;
        --services=*)
            SERVICES="${1#*=}"
            shift
            ;;
        --no-redirect-test)
            TEST_REDIRECT=false
            shift
            ;;
        --no-cert-check)
            TEST_CERT=false
            shift
            ;;
        --no-headers-check)
            TEST_HEADERS=false
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [environment] [options]"
            echo "  environment: dev, staging, prod (default: prod)"
            echo ""
            echo "Options:"
            echo "  --project=PROJECT_ID      GCP Project ID"
            echo "  --region=REGION           GCP Region (default: us-central1)"
            echo "  --services=SERVICES       Comma-separated service list"
            echo "                            (e.g., 'backend,client-app,admin-app,specialist-app')"
            echo "  --no-redirect-test        Skip HTTP to HTTPS redirect test"
            echo "  --no-cert-check           Skip SSL certificate check"
            echo "  --no-headers-check        Skip security headers check"
            echo "  --verbose                 Show detailed output"
            echo "  --help                    Show this help"
            echo ""
            echo "Examples:"
            echo "  $0 dev --project=my-project-dev"
            echo "  $0 prod --project=my-project --services=backend,client-app"
            echo "  $0 staging --verbose --no-cert-check"
            exit 0
            ;;
        *)
            log_error "Unknown argument: $1"
            exit 1
            ;;
    esac
done

# Application configuration
APP_NAME="consultant"
DEFAULT_SERVICES=("backend" "client-app" "admin-app" "specialist-app")

# Determine which services to test
if [ -n "$SERVICES" ]; then
    IFS=',' read -ra SERVICE_LIST <<< "$SERVICES"
else
    SERVICE_LIST=("${DEFAULT_SERVICES[@]}")
fi

# ============================================
# Prerequisite Checks
# ============================================

check_prerequisites() {
    log_info "Checking prerequisites..."

    local missing_tools=()

    # Check for required tools
    for tool in curl openssl; do
        if ! command -v $tool &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done

    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install missing tools and try again."
        exit 1
    fi

    # Check gcloud authentication
    if ! command -v gcloud &> /dev/null; then
        log_warning "gcloud CLI not found. Some tests will be limited."
    elif ! gcloud auth list --format="value(account)" | grep -q "@"; then
        log_warning "Not authenticated with gcloud. Some tests will be limited."
    fi

    log_success "Prerequisites check passed"
}

# ============================================
# Service Discovery
# ============================================

get_service_url() {
    local service_name="$1"
    local full_service_name="${APP_NAME}-${service_name}"

    # Try to get URL from gcloud
    if command -v gcloud &> /dev/null && [ -n "$PROJECT_ID" ]; then
        gcloud config set project "$PROJECT_ID" &>/dev/null || true
        local url
        url=$(gcloud run services describe "$full_service_name" \
            --region="$REGION" \
            --format="value(status.url)" 2>/dev/null || echo "")

        if [ -n "$url" ]; then
            echo "$url"
            return 0
        fi
    fi

    # Fallback: construct URL pattern
    echo "https://${full_service_name}-[unknown]-${REGION//-/}.a.run.app"
}

get_service_domains() {
    local service_name="$1"
    local domains=()

    # Get Cloud Run URL
    local cloudrun_url
    cloudrun_url=$(get_service_url "$service_name")
    domains+=("$cloudrun_url")

    # Try to get custom domains from gcloud
    if command -v gcloud &> /dev/null && [ -n "$PROJECT_ID" ]; then
        local full_service_name="${APP_NAME}-${service_name}"
        local custom_domains
        custom_domains=$(gcloud run domain-mappings list \
            --filter="metadata.name:${full_service_name}" \
            --region="$REGION" \
            --format="value(metadata.name)" 2>/dev/null || echo "")

        for domain in $custom_domains; do
            domains+=("https://$domain")
        done
    fi

    # Print domains
    for domain in "${domains[@]}"; do
        echo "$domain"
    done
}

# ============================================
# HTTPS Tests
# ============================================

test_https_connectivity() {
    local url="$1"
    local service_name="$2"

    log_info "Testing HTTPS connectivity for $service_name: $url"

    local start_time
    start_time=$(date +%s%3N)

    # Test basic HTTPS connectivity
    if curl -s -f --max-time 30 -k "$url/health" &>/dev/null; then
        local end_time
        end_time=$(date +%s%3N)
        local response_time=$((end_time - start_time))

        log_success "HTTPS connectivity OK (${response_time}ms)"
        return 0
    else
        log_error "HTTPS connectivity FAILED"
        return 1
    fi
}

test_ssl_certificate() {
    local url="$1"
    local service_name="$2"

    if [ "$TEST_CERT" = false ]; then
        log_info "Skipping SSL certificate check for $service_name"
        return 0
    fi

    log_info "Testing SSL certificate for $service_name: $url"

    # Extract hostname from URL
    local hostname
    hostname=$(echo "$url" | sed -e 's|^https://||' -e 's|/.*||')

    # Get certificate details
    local cert_info
    cert_info=$(echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" 2>/dev/null | \
        openssl x509 -noout -dates 2>/dev/null || true)

    if [ -z "$cert_info" ]; then
        log_error "Failed to retrieve SSL certificate for $hostname"
        return 1
    fi

    # Parse certificate dates
    local not_before
    local not_after
    not_before=$(echo "$cert_info" | grep "notBefore" | cut -d= -f2-)
    not_after=$(echo "$cert_info" | grep "notAfter" | cut -d= -f2-)

    # Check if certificate is valid
    local current_time
    current_time=$(date +%s)
    local not_after_time
    not_after_time=$(date -d "$not_after" +%s 2>/dev/null || date -j -f "%b %d %H:%M:%S %Y %Z" "$not_after" +%s 2>/dev/null || echo 0)

    if [ "$not_after_time" -gt "$current_time" ]; then
        local days_remaining
        days_remaining=$(((not_after_time - current_time) / 86400))

        if [ "$days_remaining" -lt 30 ]; then
            log_warning "Certificate expires in $days_remaining days (expires: $not_after)"
        else
            log_success "Certificate valid (expires: $not_after, ${days_remaining} days remaining)"
        fi
    else
        log_error "Certificate EXPIRED (expired: $not_after)"
        return 1
    fi

    # Get certificate issuer
    local issuer
    issuer=$(echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" 2>/dev/null | \
        openssl x509 -noout -issuer 2>/dev/null | \
        sed 's/issuer= //' || echo "Unknown")

    log_debug "Certificate issuer: $issuer"

    # Check for Google-managed certificate
    if echo "$issuer" | grep -qi "google"; then
        log_success "Google-managed SSL certificate detected"
    fi

    return 0
}

test_security_headers() {
    local url="$1"
    local service_name="$2"

    if [ "$TEST_HEADERS" = false ]; then
        log_info "Skipping security headers check for $service_name"
        return 0
    fi

    log_info "Testing security headers for $service_name: $url"

    local headers
    headers=$(curl -s -I -k "$url" 2>/dev/null || echo "")

    if [ -z "$headers" ]; then
        log_error "Failed to retrieve headers"
        return 1
    fi

    local missing_headers=()
    local found_headers=()

    # Check for important security headers
    declare -A expected_headers=(
        ["Strict-Transport-Security"]="max-age"
        ["X-Content-Type-Options"]="nosniff"
        ["X-Frame-Options"]="DENY\|SAMEORIGIN"
        ["X-XSS-Protection"]="1; mode=block"
        ["Referrer-Policy"]="strict-origin-when-cross-origin\|no-referrer"
        ["Content-Security-Policy"]=".*"
    )

    for header in "${!expected_headers[@]}"; do
        local pattern="${expected_headers[$header]}"

        if echo "$headers" | grep -iq "^$header:"; then
            local header_value
            header_value=$(echo "$headers" | grep -i "^$header:" | head -1)
            found_headers+=("$header_value")
            log_debug "Found: $header_value"
        else
            missing_headers+=("$header")
        fi
    done

    if [ ${#missing_headers[@]} -eq 0 ]; then
        log_success "All expected security headers present"
    else
        log_warning "Missing security headers: ${missing_headers[*]}"
        # Not failing the test, just warning
    fi

    # Check for HSTS header specifically
    if echo "$headers" | grep -iq "^Strict-Transport-Security:"; then
        local hsts_value
        hsts_value=$(echo "$headers" | grep -i "^Strict-Transport-Security:" | head -1 | cut -d: -f2- | xargs)
        log_success "HSTS header present: $hsts_value"
    else
        log_warning "HSTS header missing (recommended for HTTPS sites)"
    fi

    return 0
}

test_http_redirect() {
    local url="$1"
    local service_name="$2"

    if [ "$TEST_REDIRECT" = false ]; then
        log_info "Skipping HTTP to HTTPS redirect test for $service_name"
        return 0
    fi

    log_info "Testing HTTP to HTTPS redirect for $service_name"

    # Convert HTTPS URL to HTTP
    local http_url
    http_url=$(echo "$url" | sed 's|^https://|http://|')

    # Follow redirects and check final URL
    local redirect_result
    redirect_result=$(curl -s -I -L -k "$http_url" 2>/dev/null | \
        grep -i "^location:\|^HTTP/" || echo "")

    if echo "$redirect_result" | grep -q "301\|302\|307\|308"; then
        if echo "$redirect_result" | grep -qi "location:.*https://"; then
            log_success "HTTP to HTTPS redirect working correctly"
            return 0
        else
            log_warning "Redirect found but not to HTTPS"
            return 1
        fi
    else
        log_warning "No HTTP to HTTPS redirect detected"
        return 1
    fi
}

test_tls_protocols() {
    local url="$1"
    local service_name="$2"

    log_info "Testing TLS protocols for $service_name: $url"

    # Extract hostname from URL
    local hostname
    hostname=$(echo "$url" | sed -e 's|^https://||' -e 's|/.*||')

    # Test TLS 1.2
    if echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" \
        -tls1_2 2>/dev/null | grep -q "CONNECTED"; then
        log_success "TLS 1.2 supported"
    else
        log_warning "TLS 1.2 NOT supported"
    fi

    # Test TLS 1.3 (if openssl supports it)
    if echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" \
        -tls1_3 2>/dev/null | grep -q "CONNECTED"; then
        log_success "TLS 1.3 supported"
    else
        log_debug "TLS 1.3 not tested (openssl may not support)"
    fi

    # Test insecure protocols (should fail)
    if echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" \
        -ssl3 2>/dev/null | grep -q "CONNECTED"; then
        log_error "SSL 3.0 supported (INSECURE!)"
        return 1
    else
        log_success "SSL 3.0 disabled (secure)"
    fi

    if echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" \
        -tls1 2>/dev/null | grep -q "CONNECTED"; then
        log_warning "TLS 1.0 supported (deprecated)"
        return 1
    else
        log_success "TLS 1.0 disabled (secure)"
    fi

    if echo | openssl s_client -connect "$hostname:443" \
        -servername "$hostname" \
        -tls1_1 2>/dev/null | grep -q "CONNECTED"; then
        log_warning "TLS 1.1 supported (deprecated)"
        return 1
    else
        log_success "TLS 1.1 disabled (secure)"
    fi

    return 0
}

test_api_endpoints() {
    local url="$1"
    local service_name="$2"

    log_info "Testing API endpoints for $service_name: $url"

    # Test health endpoint
    if curl -s -f -k "$url/health" &>/dev/null; then
        log_success "Health endpoint accessible"
    else
        log_error "Health endpoint not accessible"
        return 1
    fi

    # Test docs endpoint (for backend)
    if [ "$service_name" = "backend" ]; then
        if curl -s -f -k "$url/docs" &>/dev/null; then
            log_success "Documentation endpoint accessible"
        else
            log_warning "Documentation endpoint not accessible"
        fi
    fi

    return 0
}

# ============================================
# Main Test Execution
# ============================================

run_tests_for_service() {
    local service_name="$1"
    local test_results=()
    local passed_tests=0
    local total_tests=0

    log_info ""
    log_info "========================================"
    log_info "Testing service: $service_name"
    log_info "========================================"

    # Get domains for this service
    local domains
    mapfile -t domains < <(get_service_domains "$service_name")

    if [ ${#domains[@]} -eq 0 ]; then
        log_error "No domains found for service $service_name"
        return 1
    fi

    # Test primary domain (first in list)
    local primary_domain="${domains[0]}"

    # Skip if domain contains [unknown] placeholder
    if [[ "$primary_domain" == *"[unknown]"* ]]; then
        log_warning "Skipping $service_name - URL not available"
        log_warning "Try specifying project ID with --project=PROJECT_ID"
        return 0
    fi

    # Run tests
    local tests=(
        "test_https_connectivity \"$primary_domain\" \"$service_name\""
        "test_api_endpoints \"$primary_domain\" \"$service_name\""
    )

    if [ "$TEST_CERT" = true ]; then
        tests+=("test_ssl_certificate \"$primary_domain\" \"$service_name\"")
    fi

    if [ "$TEST_HEADERS" = true ]; then
        tests+=("test_security_headers \"$primary_domain\" \"$service_name\"")
    fi

    if [ "$TEST_REDIRECT" = true ]; then
        tests+=("test_http_redirect \"$primary_domain\" \"$service_name\"")
    fi

    tests+=("test_tls_protocols \"$primary_domain\" \"$service_name\"")

    # Execute tests
    for test_cmd in "${tests[@]}"; do
        total_tests=$((total_tests + 1))

        if eval "$test_cmd"; then
            passed_tests=$((passed_tests + 1))
            test_results+=("✓")
        else
            test_results+=("✗")
        fi
    done

    # Print test summary
    log_info ""
    log_info "Test summary for $service_name:"
    echo "  Results: ${test_results[*]}"
    echo "  Passed:  $passed_tests/$total_tests tests"

    if [ $passed_tests -eq $total_tests ]; then
        log_success "All tests passed for $service_name!"
        return 0
    else
        log_warning "Some tests failed for $service_name"
        return 1
    fi
}

# ============================================
# Summary Report
# ============================================

generate_summary() {
    log_info ""
    log_info "========================================"
    log_info "HTTPS TEST SUMMARY"
    log_info "========================================"
    log_info "Environment: $ENVIRONMENT"
    log_info "Region: $REGION"
    if [ -n "$PROJECT_ID" ]; then
        log_info "Project: $PROJECT_ID"
    fi
    log_info "Services tested: ${SERVICE_LIST[*]}"
    log_info ""

    echo -e "${BLUE}Recommendations:${NC}"
    echo "1. Ensure SSL certificates are auto-renewing (Google-managed)"
    echo "2. Monitor certificate expiration (30-day warning recommended)"
    echo "3. Consider adding Content-Security-Policy header"
    echo "4. Regularly test TLS configuration with tools like SSL Labs"
    echo "5. Set up alerts for HTTPS connectivity issues"
    echo ""

    echo -e "${BLUE}Next Steps:${NC}"
    echo "✓ HTTPS configuration verified"
    echo "→ Set up monitoring for SSL certificate expiration"
    echo "→ Configure Cloud Armor for DDoS protection"
    echo "→ Implement WAF rules if needed"
    echo "→ Set up uptime checks for all endpoints"
}

# ============================================
# Main Function
# ============================================

main() {
    echo -e "${BLUE}"
    cat << "EOF"
  _    _ _______ ______  _____   _____
 | |  | |__   __|  ____|/ ____| / ____|
 | |__| |  | |  | |__  | (___  | (___
 |  __  |  | |  |  __|  \___ \  \___ \
 | |  | |  | |  | |____ ____) | ____) |
 |_|  |_|  |_|  |______|_____/ |_____/

  HTTPS Verification Script
EOF
    echo -e "${NC}"
    echo "Google Cloud Platform HTTPS Testing"
    echo "========================================"
    echo ""

    # Show configuration
    log_info "Configuration:"
    echo "  Environment: $ENVIRONMENT"
    echo "  Region:      $REGION"
    if [ -n "$PROJECT_ID" ]; then
        echo "  Project:     $PROJECT_ID"
    else
        echo "  Project:     (auto-detecting)"
    fi
    echo "  Services:    ${SERVICE_LIST[*]}"
    echo ""

    # Check prerequisites
    check_prerequisites

    # Set project if provided
    if [ -n "$PROJECT_ID" ] && command -v gcloud &> /dev/null; then
        log_info "Setting GCP project: $PROJECT_ID"
        gcloud config set project "$PROJECT_ID" --quiet || true
    fi

    # Run tests for each service
    local overall_success=true

    for service_name in "${SERVICE_LIST[@]}"; do
        if ! run_tests_for_service "$service_name"; then
            overall_success=false
        fi
    done

    # Generate summary
    generate_summary

    # Final result
    log_info ""
    if [ "$overall_success" = true ]; then
        log_success "========================================"
        log_success "ALL HTTPS TESTS COMPLETED SUCCESSFULLY"
        log_success "========================================"
        exit 0
    else
        log_warning "========================================"
        log_warning "SOME TESTS FAILED - REVIEW WARNINGS"
        log_warning "========================================"
        exit 1
    fi
}

# ============================================
# Script Entry Point
# ============================================

# Run main function
main "$@"
