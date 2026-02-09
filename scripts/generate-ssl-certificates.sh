#!/bin/bash

# SSL/TLS Certificate Generation Script
# Generates self-signed certificates for development or uses Let's Encrypt for production

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CERTS_DIR="$PROJECT_ROOT/certs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}SSL/TLS Certificate Generation${NC}"
echo "================================"
echo ""

# Create certs directory if it doesn't exist
mkdir -p "$CERTS_DIR"
echo -e "${GREEN}✓${NC} Created certs directory: $CERTS_DIR"

# Check if certificates already exist
if [[ -f "$CERTS_DIR/certificate.crt" ]] && [[ -f "$CERTS_DIR/private.key" ]]; then
    echo -e "${YELLOW}⚠${NC} Certificates already exist in $CERTS_DIR"
    read -p "Do you want to regenerate them? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Using existing certificates."
        exit 0
    else
        rm -f "$CERTS_DIR/certificate.crt" "$CERTS_DIR/private.key"
        echo -e "${GREEN}✓${NC} Removed old certificates"
    fi
fi

# Determine environment
read -p "Generate certificates for (d)evelopment or (p)roduction? [d/p] " -n 1 -r
echo
ENV_TYPE="${REPLY:-d}"

if [[ "$ENV_TYPE" =~ ^[Dd]$ ]]; then
    echo -e "${YELLOW}Generating self-signed certificate for development...${NC}"
    
    # Self-signed certificate valid for 365 days
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$CERTS_DIR/private.key" \
        -out "$CERTS_DIR/certificate.crt" \
        -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
    
    echo -e "${GREEN}✓${NC} Self-signed certificate generated successfully"
    echo ""
    echo "Certificate Details:"
    echo "  - Path: $CERTS_DIR"
    echo "  - Valid for: 365 days"
    echo "  - Common Name: localhost"
    echo "  - Type: Self-signed (for development only)"
    echo ""
    echo -e "${YELLOW}⚠${NC} Warning: Self-signed certificates will trigger browser warnings."
    echo "   For production, use Let's Encrypt or a trusted CA."
    
elif [[ "$ENV_TYPE" =~ ^[Pp]$ ]]; then
    echo -e "${YELLOW}Production Setup - Let's Encrypt${NC}"
    echo ""
    echo "To use Let's Encrypt with Certbot:"
    echo ""
    echo "1. Install Certbot:"
    echo "   sudo apt-get install certbot python3-certbot-nginx"
    echo ""
    echo "2. Get certificates:"
    echo "   sudo certbot certonly --standalone -d yourdomain.com"
    echo ""
    echo "3. Copy certificates to ./certs:"
    echo "   sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./certs/certificate.crt"
    echo "   sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./certs/private.key"
    echo "   sudo chown $(whoami):$(whoami) ./certs/*"
    echo ""
    echo "4. Set up auto-renewal:"
    echo "   sudo certbot renew --dry-run"
    echo ""
    echo -e "${RED}Note:${NC} Update nginx.conf server_name from 'localhost' to 'yourdomain.com'"
    exit 0
    
else
    echo -e "${RED}✗${NC} Invalid option. Please choose 'd' for development or 'p' for production."
    exit 1
fi

# Verify certificates
if [[ ! -f "$CERTS_DIR/certificate.crt" ]] || [[ ! -f "$CERTS_DIR/private.key" ]]; then
    echo -e "${RED}✗${NC} Certificate generation failed"
    exit 1
fi

# Set permissions
chmod 644 "$CERTS_DIR/certificate.crt"
chmod 600 "$CERTS_DIR/private.key"
echo -e "${GREEN}✓${NC} Permissions set correctly"

echo ""
echo -e "${GREEN}✓${NC} SSL/TLS certificates are ready!"
echo ""
echo "Next steps:"
echo "1. Ensure certs directory is mounted in docker-compose.yml"
echo "2. Run: docker-compose up --build"
echo "3. Access your application at: https://localhost"
echo ""
