# Production HTTPS Deployment Guide

Complete guide for deploying the Consultant application with trusted HTTPS certificates in production.

---

## 🚀 Quick Start: Deploy with Namecheap Domain + Let's Encrypt

**Already have a domain on Namecheap?** Follow these exact steps:

### Step 1: Get Your Server IP Address

```bash
# Your server should have a public IP
# For VPS providers (DigitalOcean, Linode, Vultr, AWS EC2, etc.)
# Example: 192.168.1.100

# Check your server IP
curl ifconfig.me
# or
hostname -I
```

**Write down your server IP:** `YOUR_SERVER_IP`

---

### Step 2: Configure DNS in Namecheap

1. **Login to Namecheap:** https://www.namecheap.com/myaccount/login/

2. **Go to Domain List:**
   - Click "Domain List" in the left sidebar
   - Click "Manage" next to your domain (e.g., `consultant.com`)

3. **Navigate to Advanced DNS:**
   - Click on "Advanced DNS" tab

4. **Add DNS Records:**

Click "Add New Record" and add these **A Records**:

| Type | Host | Value | TTL |
|------|------|-------|-----|
| `A Record` | `@` | `YOUR_SERVER_IP` | `Automatic` |
| `A Record` | `www` | `YOUR_SERVER_IP` | `Automatic` |

**Example:**
```
Type: A Record
Host: @
Value: 192.168.1.100
TTL: Automatic

Type: A Record
Host: www
Value: 192.168.1.100
TTL: Automatic
```

5. **Save Changes:**
   - Click the green checkmark ✓ to save each record
   - Wait 5-10 minutes for DNS propagation

6. **Verify DNS Propagation:**
```bash
# Wait a few minutes, then test:
ping consultant.com
# Should show your server IP

# Or use online tool:
# https://dnschecker.org/
```

---

### Step 3: Configure Server Firewall

```bash
# Allow HTTP and HTTPS traffic
sudo ufw allow 80/tcp    # HTTP (for certificate validation)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 22/tcp    # SSH (don't lock yourself out!)

# Enable firewall
sudo ufw enable

# Verify
sudo ufw status
```

**Expected output:**
```
Status: active
To                         Action      From
--                         ------      ----
80/tcp                     ALLOW       Anywhere
443/tcp                    ALLOW       Anywhere
22/tcp                     ALLOW       Anywhere
```

---

### Step 4: Install Certbot on Server

```bash
# SSH into your server
ssh user@YOUR_SERVER_IP

# Update package list
sudo apt-get update

# Install Certbot
sudo apt-get install -y certbot
```

---

### Step 5: Obtain SSL Certificate

```bash
# Stop any service using port 80
sudo docker stop consultant-nginx 2>/dev/null || true

# Get certificate (replace with your domain)
sudo certbot certonly --standalone \
  -d consultant.com \
  -d www.consultant.com
```

**When prompted:**
- Enter your email address (for renewal notifications)
- Agree to Terms of Service (type `A`)
- Choose whether to share email with EFF (your choice)

**Expected output:**
```
Congratulations! Your certificate and chain have been saved at:
  /etc/letsencrypt/live/consultant.com/fullchain.pem
  /etc/letsencrypt/live/consultant.com/privkey.pem

Your key file has been saved at:
  /etc/letsencrypt/live/consultant.com/privkey.pem

Your cert will expire on 2026-05-30
```

---

### Step 6: Copy Certificates to Project

```bash
# Navigate to project directory
cd /home/lvn/prg/scala/Consultant/backend

# Create certs directory
mkdir -p certs

# Copy certificates from Let's Encrypt
sudo cp /etc/letsencrypt/live/consultant.com/fullchain.pem ./certs/certificate.crt
sudo cp /etc/letsencrypt/live/consultant.com/privkey.pem ./certs/private.key

# Set correct ownership and permissions
sudo chown lvn:lvn ./certs/*
chmod 644 ./certs/certificate.crt
chmod 600 ./certs/private.key

# Verify files exist
ls -la certs/
```

**Expected output:**
```
total 16
drwxr-xr-x  2 lvn lvn 4096 Mar  1 14:00 .
drwxr-xr-x 23 lvn lvn 4096 Mar  1 14:00 ..
-rw-r--r--  1 lvn lvn 5423 Mar  1 14:00 certificate.crt
-rw-------  1 lvn lvn 1704 Mar  1 14:00 private.key
```

---

### Step 7: Update Nginx Configuration

Edit `nginx.conf`:

```bash
cd /home/lvn/prg/scala/Consultant/backend
nano nginx.conf
# or
vim nginx.conf
```

**Find and replace:**

```nginx
# OLD (development):
server_name localhost;

# NEW (production):
server_name consultant.com www.consultant.com;
```

**Also update HTTP redirect server:**

```nginx
# OLD:
server {
    listen 80;
    server_name localhost;
    return 301 https://$host$request_uri;
}

# NEW:
server {
    listen 80;
    server_name consultant.com www.consultant.com;
    return 301 https://consultant.com$request_uri;
}
```

**Save and exit** (in nano: `Ctrl+O`, `Enter`, `Ctrl+X`)

---

### Step 8: Update Environment Variables

Create or update `.env` file:

```bash
cd /home/lvn/prg/scala/Consultant/backend
nano .env
```

**Add production settings:**

```env
# Server
SERVER_HOST=0.0.0.0
SERVER_PORT=8090

# Database (update with your production database)
DB_URL=jdbc:postgresql://consultant-db:5432/consultant
DB_USER=consultant_user
DB_PASSWORD=consultant_pass

# Security - IMPORTANT: Generate new secrets for production!
JWT_SECRET=<run-this-command-to-generate>
# openssl rand -base64 64

SESSION_SECRET=<run-this-command-to-generate>
# openssl rand -base64 32

DB_ENCRYPTION_KEY=<run-this-command-to-generate>
# openssl rand -base64 32

# CORS - Your production domain
CORS_ORIGINS=https://consultant.com,https://www.consultant.com

# HTTPS Settings
FORCE_HTTPS=true
SECURE_COOKIES=true
SESSION_SECURE=true

# JWT Configuration
JWT_ISSUER=consultant-api
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d

# Security Settings
MAX_LOGIN_ATTEMPTS=5
ACCOUNT_LOCK_DURATION_MINUTES=15
```

**Generate secure secrets:**

```bash
# Run these commands and copy the output to .env
openssl rand -base64 64  # For JWT_SECRET
openssl rand -base64 32  # For SESSION_SECRET
openssl rand -base64 32  # For DB_ENCRYPTION_KEY
```

---

### Step 9: Deploy with Docker Compose

```bash
cd /home/lvn/prg/scala/Consultant/backend

# Build all images
docker-compose -f docker-compose.app.yml -f docker-compose.prod.yml build

# Start all services
docker-compose -f docker-compose.app.yml -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.app.yml -f docker-compose.prod.yml ps
```

**Expected output:**
```
NAME                      COMMAND                  STATUS              PORTS
consultant-app-1          "sh -c 'java $JAVA_O…"   Up 10 seconds       0.0.0.0:8081->8090/tcp
consultant-app-2          "sh -c 'java $JAVA_O…"   Up 10 seconds       0.0.0.0:8082->8090/tcp
consultant-app-3          "sh -c 'java $JAVA_O…"   Up 10 seconds       0.0.0.0:8083->8090/tcp
consultant-backend        "sh -c 'java $JAVA_O…"   Up 10 seconds       0.0.0.0:8090->8090/tcp
consultant-db             "docker-entrypoint.s…"   Up 10 seconds       5432/tcp
consultant-nginx          "/docker-entrypoint.…"   Up 10 seconds       0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
```

---

### Step 10: Test Your Production Site

```bash
# Test HTTPS (should work WITHOUT -k flag now!)
curl https://consultant.com/health

# Test HTTP redirect
curl -I http://consultant.com
# Should return: HTTP/1.1 301 Moved Permanently
# Location: https://consultant.com/

# Test security headers
curl -I https://consultant.com | grep -E "(Strict-Transport-Security|X-Frame-Options)"
```

**Open in browser:**
- https://consultant.com
- https://www.consultant.com

✅ **No security warnings!**

---

### Step 11: Set Up Auto-Renewal

```bash
# Create renewal script
sudo nano /usr/local/bin/renew-cert.sh
```

**Add this content:**

```bash
#!/bin/bash
certbot renew --quiet
docker restart consultant-nginx
```

**Make executable:**

```bash
sudo chmod +x /usr/local/bin/renew-cert.sh
```

**Add to crontab:**

```bash
sudo crontab -e
```

**Add this line:**

```cron
# Renew Let's Encrypt certificate daily at 2:30 AM
30 2 * * * /usr/local/bin/renew-cert.sh
```

**Test renewal:**

```bash
sudo certbot renew --dry-run
```

---

### Step 12: Verify Everything Works

**Online Testing Tools:**

1. **SSL Labs Test:** https://www.ssllabs.com/ssltest/
   - Enter: `consultant.com`
   - Target grade: A or A+

2. **Security Headers:** https://securityheaders.com/
   - Enter: `https://consultant.com`
   - Target grade: A or A+

3. **Why No Padlock:** https://www.whynopadlock.com/
   - Check for mixed content issues

**Browser Check:**
- Open https://consultant.com
- Click the padlock icon in address bar
- Should show "Connection is secure"
- Certificate should be valid and trusted

---

## 🎉 Congratulations!

Your production deployment is complete!

**What you have now:**
- ✅ Trusted HTTPS (no browser warnings)
- ✅ Auto-renewing SSL certificate
- ✅ HTTP → HTTPS redirect
- ✅ Security headers configured
- ✅ Load balancing (3 backend instances)

**Access URLs:**
- Main site: https://consultant.com
- Admin app: https://consultant.com/admin/
- Client app: https://consultant.com/client/
- Specialist app: https://consultant.com/specialist/

---

## 🔧 Maintenance

### Renew Certificate Manually

```bash
sudo certbot renew --force-renewal
sudo docker restart consultant-nginx
```

### Check Certificate Expiry

```bash
sudo certbot certificates
```

### View Logs

```bash
# Nginx logs
docker logs consultant-nginx

# Backend logs
docker logs consultant-backend

# Certbot logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log
```

### Update Application

```bash
# Pull latest changes
git pull

# Rebuild and restart
docker-compose -f docker-compose.app.yml -f docker-compose.prod.yml up -d --build
```

---

---

## Option 2: AWS Certificate Manager (FREE)

**Best for:** Applications hosted on AWS

### Prerequisites

- AWS Account
- Domain registered in Route 53 (or DNS access)
- Application hosted on AWS (ECS, EC2, Load Balancer)

### Step 1: Request Certificate

```bash
# Via AWS Console:
# 1. Go to AWS Certificate Manager
# 2. Click "Request a certificate"
# 3. Choose "Request a public certificate"
# 4. Add domain names: consultant.com, www.consultant.com
# 5. Choose validation method: DNS
```

### Step 2: Validate Domain

```bash
# AWS will provide CNAME records
# Add them to your DNS configuration:
# _1234567890abcdef.consultant.com → _abcdef1234567890.acm-validations.aws
```

### Step 3: Attach to Load Balancer

```bash
# Via AWS Console:
# 1. Go to EC2 → Load Balancers
# 2. Select your load balancer
# 3. Listeners → Edit
# 4. HTTPS:443 → Select your certificate
```

### Step 4: Update Application

```yaml
# docker-compose.prod.yml
services:
  nginx:
    environment:
      - DOMAIN_NAME=consultant.com
    # Certificate managed by AWS ALB
```

---

## Option 3: Cloudflare SSL (FREE)

**Best for:** Applications using Cloudflare CDN

### Step 1: Add Site to Cloudflare

```bash
# 1. Create Cloudflare account
# 2. Add your site: consultant.com
# 3. Update nameservers at your domain registrar
```

### Step 2: Enable SSL/TLS

```bash
# In Cloudflare Dashboard:
# 1. Go to SSL/TLS
# 2. Choose encryption mode: Full (strict)
# 3. Cloudflare will automatically issue certificate
```

### Step 3: Configure Origin Certificate (Optional)

```bash
# For end-to-end encryption:
# 1. SSL/TLS → Origin Server → Create Certificate
# 2. Download certificate and key
# 3. Install on your server
```

---

## Option 4: Paid SSL Certificate

**Best for:** Enterprises, Extended Validation (EV) requirements

### Providers

| Provider | Type | Cost/Year |
|----------|------|-----------|
| **DigiCert** | Standard SSL | $199+ |
| **Comodo** | Standard SSL | $79+ |
| **GlobalSign** | Standard SSL | $149+ |
| **Sectigo** | Standard SSL | $89+ |

### When to Pay for SSL

- Need Extended Validation (EV) certificate (shows company name in browser)
- Enterprise compliance requirements
- Need warranty/insurance coverage
- Need 1-2 year validity without renewal
- Dedicated support required

### Installation

```bash
# After purchasing, you'll receive:
# - Certificate file (.crt or .pem)
# - Private key (.key)
# - CA Bundle/Intermediate certificates (.ca-bundle)

# Copy to project
cp downloaded_certificate.crt ./certs/certificate.crt
cp private_key.key ./certs/private.key
cp ca_bundle.ca-bundle ./certs/ca-bundle.crt

# Update nginx.conf to include CA bundle
server {
    listen 443 ssl http2;
    
    ssl_certificate /etc/nginx/certs/certificate.crt;
    ssl_certificate_key /etc/nginx/certs/private.key;
    ssl_trusted_certificate /etc/nginx/certs/ca-bundle.crt;
    
    # ... rest of configuration
}
```

---

## Production Deployment Checklist

### Before Deployment

- [ ] Domain name purchased and configured
- [ ] DNS records pointing to server
- [ ] Server firewall configured (ports 80, 443 open)
- [ ] SSL certificate obtained and installed
- [ ] Auto-renewal configured (for Let's Encrypt)
- [ ] Environment variables updated for production
- [ ] Database backups configured
- [ ] Monitoring and alerting set up

### Environment Variables (Production)

```env
# .env.production
# Server
SERVER_HOST=0.0.0.0
SERVER_PORT=8090

# Database (use managed service in production)
DB_URL=jdbc:postgresql://your-rds-instance.amazonaws.com:5432/consultant
DB_USER=consultant_user
DB_PASSWORD=<strong-production-password>
DB_ENCRYPTION_KEY=<production-encryption-key-32-chars>

# Security - Generate strong random values!
JWT_SECRET=<generate-64-character-random-secret>
SESSION_SECRET=<generate-32-character-random-secret>

# CORS (your production domain)
CORS_ORIGINS=https://consultant.com,https://www.consultant.com

# HTTPS Settings
FORCE_HTTPS=true
SECURE_COOKIES=true
SESSION_SECURE=true

# JWT Configuration
JWT_ISSUER=consultant-api
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d

# Security Settings
MAX_LOGIN_ATTEMPTS=5
ACCOUNT_LOCK_DURATION_MINUTES=15
```

### Nginx Production Configuration

```nginx
events {
    worker_connections 4096;
}

http {
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_conn_zone $binary_remote_addr zone=addr:10m;

    # Caching
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m 
                     max_size=100m inactive=60m;

    upstream consultant_backend {
        least_conn;
        server consultant-app-1:8090 max_fails=3 fail_timeout=30s;
        server consultant-app-2:8090 max_fails=3 fail_timeout=30s;
        server consultant-app-3:8090 max_fails=3 fail_timeout=30s;
    }

    # HTTP → HTTPS redirect
    server {
        listen 80;
        server_name consultant.com www.consultant.com;
        return 301 https://$host$request_uri;
    }

    # HTTPS server
    server {
        listen 443 ssl http2;
        server_name consultant.com www.consultant.com;

        # SSL Certificates
        ssl_certificate /etc/nginx/certs/certificate.crt;
        ssl_certificate_key /etc/nginx/certs/private.key;

        # SSL/TLS Configuration (Production-grade)
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers on;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;
        ssl_session_tickets off;

        # HSTS (HTTP Strict Transport Security)
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
        
        # Security Headers
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

        # Compression
        gzip on;
        gzip_types application/json text/plain text/css application/javascript;
        gzip_min_length 1000;
        gzip_vary on;

        # Rate limiting
        limit_req zone=api_limit burst=200 nodelay;
        limit_conn addr 10;

        # API endpoints
        location /api/ {
            proxy_pass http://consultant_backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # Health check
        location /health {
            proxy_pass http://consultant_backend/health;
            access_log off;
        }
    }
}
```

---

## Testing HTTPS Deployment

### Verify Certificate

```bash
# Check certificate details
openssl x509 -in certs/certificate.crt -text -noout

# Verify certificate chain
openssl verify -CAfile /etc/letsencrypt/live/consultant.com/chain.pem certs/certificate.crt
```

### Test HTTPS Access

```bash
# Test HTTPS endpoint (should work without -k flag now)
curl https://consultant.com/health

# Test HTTP redirect
curl -I http://consultant.com
# Should return: HTTP/1.1 301 Moved Permanently
# Location: https://consultant.com/

# Test security headers
curl -I https://consultant.com | grep -E "(Strict-Transport-Security|X-Frame-Options|X-Content-Type-Options)"
```

### Online SSL Testing Tools

- **SSL Labs Test:** https://www.ssllabs.com/ssltest/
- **Why No Padlock:** https://www.whynopadlock.com/
- **Security Headers:** https://securityheaders.com/

---

## Troubleshooting

### Issue: Certificate not trusted

**Solution:**
```bash
# Verify certificate chain is complete
sudo certbot certificates

# Reinstall certificate if needed
sudo certbot reinstall --cert-name consultant.com
```

### Issue: Auto-renewal failed

**Solution:**
```bash
# Check certbot logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log

# Manual renewal
sudo certbot renew --force-renewal

# Verify nginx config before restart
sudo nginx -t
sudo docker restart consultant-nginx
```

### Issue: Mixed content warnings

**Solution:**
- Ensure all resources (CSS, JS, images) use HTTPS or protocol-relative URLs
- Update frontend API base URLs to use HTTPS
- Check database for any hardcoded HTTP URLs

### Issue: HSTS preventing access

**Solution:**
```bash
# For testing, you can temporarily disable HSTS in nginx.conf
# Remove or comment out the HSTS header line
# add_header Strict-Transport-Security ...

# Clear HSTS in browser:
# Chrome: chrome://net-internals/#hsts
# Firefox: about:config → network.stricttransportsecurity.preloadlist
```

---

## Cost Summary

### Minimum Production Setup

| Item | Cost |
|------|------|
| **Domain name** | ~$12/year |
| **SSL Certificate** | Free (Let's Encrypt) |
| **Server (VPS)** | ~$5-20/month |
| **Total** | ~$72-252/year |

### With AWS (Serverless)

| Item | Cost |
|------|------|
| **Domain name** | ~$12/year |
| **SSL Certificate** | Free (AWS ACM) |
| **AWS Fargate/Lambda** | Pay per use |
| **AWS RDS** | ~$15/month+ |
| **Total** | Variable |

---

## Next Steps

1. ✅ **Choose SSL method** (Let's Encrypt recommended)
2. ✅ **Purchase domain** (if not already done)
3. ✅ **Configure DNS**
4. ✅ **Obtain and install certificate**
5. ✅ **Update production configuration**
6. ✅ **Set up auto-renewal**
7. ✅ **Test thoroughly**
8. ✅ **Monitor and maintain**

---

## References

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Certbot User Guide](https://eff-certbot.readthedocs.io/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [OWASP TLS Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
- [SSL Labs Testing Tool](https://www.ssllabs.com/ssltest/)

---

**Last Updated:** March 2026  
**Version:** 1.0
