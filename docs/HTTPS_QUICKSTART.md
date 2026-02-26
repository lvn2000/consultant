# HTTPS Setup Quick Start

## 🔐 What's Changed

Your system has been updated to support HTTPS with the following enhancements:

### 1. **Nginx Configuration**
- ✅ HTTP → HTTPS redirect (automatic)
- ✅ SSL/TLS certificates support
- ✅ Security headers (HSTS, X-Frame-Options, CSP, etc.)
- ✅ HTTP/2 support
- ✅ Modern cipher suites (TLSv1.2 and TLSv1.3)

### 2. **Backend Configuration**
- ✅ `FORCE_HTTPS=true` - Enforce HTTPS in all app instances
- ✅ `SECURE_COOKIES=true` - Cookies only sent over HTTPS
- ✅ `SESSION_SECURE=true` - Sessions marked as secure

### 3. **Docker Compose**
- ✅ Nginx now exposes ports 80 (redirect) and 443 (HTTPS)
- ✅ Certificate volume mounted at `/etc/nginx/certs`
- ✅ Health check for both HTTP and HTTPS

---

## 🚀 Quick Start (Development)

### Step 1: Generate Self-Signed Certificate

```bash
# Navigate to project directory
cd /home/lvn/prg/scala/Consultant/backend

# Run certificate generation script
./scripts/generate-ssl-certificates.sh

# Choose 'd' for development (self-signed)
# Choose 'p' for production (Let's Encrypt instructions)
```

**Expected output:**
```
✓ Created certs directory
✓ Self-signed certificate generated successfully
✓ Permissions set correctly
✓ SSL/TLS certificates are ready!
```

### Step 2: Build and Start Services

```bash
# Build Docker images
docker-compose build

# Start all services (with HTTPS enabled)
docker-compose up -d
```

### Step 3: Test HTTPS Access

```bash
# Test with self-signed certificate (ignore warnings)
curl -k https://localhost/health

# Response:
# {"status":"UP"}

# Test HTTP redirect
curl -i http://localhost
# Returns: HTTP/1.1 301 Moved Permanently
# Location: https://localhost/...
```

---

## 📋 Verification Checklist

After starting services, verify:

```bash
# ✓ Check Nginx is running
docker ps | grep nginx

# ✓ Check certificate files exist
ls -la certs/

# ✓ Verify certificate details
openssl x509 -in certs/certificate.crt -text -noout

# ✓ Test HTTPS endpoint
curl -k https://localhost/api/health -v

# ✓ Test HTTP redirect
curl -I http://localhost

# ✓ Check security headers
curl -k https://localhost -I | grep -i "Strict-Transport-Security"
```

---

## 📁 Certificate Locations

| Environment | Certificate Path | Type |
|---|---|---|
| **Development** | `./certs/certificate.crt` | Self-signed |
| **Development** | `./certs/private.key` | 2048-bit RSA |
| **Production** | `/etc/letsencrypt/live/yourdomain.com/fullchain.pem` | Let's Encrypt |
| **Production** | `/etc/letsencrypt/live/yourdomain.com/privkey.pem` | Let's Encrypt |

---

## ⚙️ Production Setup (Let's Encrypt)

### Prerequisites
- Domain name pointing to your server
- Ports 80 and 443 accessible from internet

### Installation

```bash
# 1. Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# 2. Generate certificate (standalone mode)
sudo certbot certonly --standalone -d yourdomain.com

# 3. Stop Docker services temporarily
docker-compose down

# 4. Copy certificates to ./certs
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./certs/certificate.crt
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./certs/private.key

# 5. Fix permissions
sudo chown $(whoami):$(whoami) ./certs/*
chmod 644 ./certs/certificate.crt
chmod 600 ./certs/private.key

# 6. Update nginx.conf
# Change: server_name localhost;
# To:     server_name yourdomain.com;

# 7. Restart services
docker-compose build
docker-compose up -d

# 8. Set up auto-renewal (optional but recommended)
sudo certbot renew --dry-run
```

### Auto-Renewal Setup

Create a cron job for automatic renewal:

```bash
# Edit crontab
sudo crontab -e

# Add this line (runs daily at 2:30 AM)
30 2 * * * certbot renew --quiet && docker-compose restart nginx
```

---

## 🔒 Security Features Enabled

### HTTP Strict Transport Security (HSTS)
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```
- Browsers remember to use HTTPS for 1 year
- Applies to all subdomains
- Can be added to HSTS preload list

### Content Security Policy
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

### Default Ciphers
```
TLSv1.2 and TLSv1.3 only
HIGH:!aNULL:!MD5
```

---

## 🐛 Troubleshooting

### Issue: "Certificate verification failed"
**Solution (Development only):**
```bash
# Use the -k flag to skip certificate verification
curl -k https://localhost/health
```

### Issue: "Connection refused on port 443"
**Solution:**
```bash
# Check if Nginx is running
docker-compose ps

# Check Nginx logs
docker-compose logs nginx

# Verify certificates exist
ls -la certs/
```

### Issue: "Redirect loop"
**Solution:**
- Check that `FORCE_HTTPS=true` is NOT combined with reverse proxy
- Ensure `X-Forwarded-Proto` header is set correctly in Nginx

### Issue: "Self-signed certificate warnings in browsers"
**This is expected for development.**
- In Chrome: Click "Advanced" then "Proceed to localhost (unsafe)"
- In Firefox: Click "Advanced" then "Accept the Risk and Continue"
- For production: Use Let's Encrypt (above)

---

## 📝 Configuration Files Modified

### Updated Files:
1. **nginx.conf** - Added HTTPS block and HTTP redirect
2. **docker-compose.yml** - Added HTTPS port and certificate volume
3. **scripts/generate-ssl-certificates.sh** - New certificate generation script

### Environment Variables:
```yaml
FORCE_HTTPS: "true"           # Enforce HTTPS
SECURE_COOKIES: "true"        # Cookies over HTTPS only
SESSION_SECURE: "true"        # Secure session flag
```

---

## 📚 Next Steps

1. ✅ **Generate certificates** → Run `./scripts/generate-ssl-certificates.sh`
2. ✅ **Start services** → `docker-compose up -d`
3. ✅ **Test access** → `curl -k https://localhost/health`
4. ✅ **Update frontend** → Set API base to `https://localhost` in Nuxt configs
5. ✅ **For production** → Follow Let's Encrypt setup above

---

## 🔗 References

- [Nginx SSL/TLS](https://nginx.org/en/docs/http/ngx_http_ssl_module.html)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [OWASP TLS Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)

