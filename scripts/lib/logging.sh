#!/bin/bash

# ============================================
# Logging Module
# ============================================
# Provides centralized logging functions for deployment scripts
# Usage: source ./lib/logging.sh

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Log levels
LOG_LEVEL_DEBUG=0
LOG_LEVEL_INFO=1
LOG_LEVEL_WARNING=2
LOG_LEVEL_ERROR=3

# Default log level
LOG_LEVEL=${LOG_LEVEL:-$LOG_LEVEL_INFO}

# Enable/disable timestamps
LOG_WITH_TIMESTAMPS=${LOG_WITH_TIMESTAMPS:-false}

# ============================================
# Internal logging function
# ============================================
_log() {
    local level=$1
    local level_name=$2
    local color=$3
    shift 3
    local message="$*"

    local timestamp=""
    if [[ "$LOG_WITH_TIMESTAMPS" == "true" ]]; then
        timestamp="$(date '+%Y-%m-%d %H:%M:%S') "
    fi

    echo -e "${timestamp}${color}[${level_name}]${NC} $message" >&2
}

# ============================================
# Public logging functions
# ============================================

log_debug() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_DEBUG ]]; then
        _log "$LOG_LEVEL_DEBUG" "DEBUG" "$CYAN" "$@"
    fi
}

log_info() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_INFO ]]; then
        _log "$LOG_LEVEL_INFO" "INFO" "$BLUE" "$@"
    fi
}

log_success() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_INFO ]]; then
        _log "$LOG_LEVEL_INFO" "SUCCESS" "$GREEN" "$@"
    fi
}

log_warning() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_WARNING ]]; then
        _log "$LOG_LEVEL_WARNING" "WARNING" "$YELLOW" "$@"
    fi
}

log_error() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_ERROR ]]; then
        _log "$LOG_LEVEL_ERROR" "ERROR" "$RED" "$@"
    fi
    exit 1
}

log_error_continue() {
    if [[ $LOG_LEVEL -le $LOG_LEVEL_ERROR ]]; then
        _log "$LOG_LEVEL_ERROR" "ERROR" "$RED" "$@"
    fi
}

# ============================================
# Utility functions
# ============================================

log_section() {
    local title=$1
    echo ""
    echo -e "${MAGENTA}========================================${NC}"
    echo -e "${MAGENTA}${title}${NC}"
    echo -e "${MAGENTA}========================================${NC}"
    echo ""
}

log_step() {
    local step_num=$1
    local step_desc=$2
    echo -e "${CYAN}[Step $step_num]${NC} $step_desc"
}

log_variable() {
    local var_name=$1
    local var_value=$2
    echo -e "${BLUE}  ├─${NC} ${var_name}: ${CYAN}${var_value}${NC}"
}
