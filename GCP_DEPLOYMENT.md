# Google Cloud Platform (GCP) Deployment Guide

This guide provides comprehensive instructions for deploying the Consultant Platform to Google Cloud Platform (GCP) using the automated deployment script.

## Architecture Overview

The deployment creates the following resources in your GCP project:

```
Google Cloud Project
├── Cloud Run Service: consultant-backend (Scala/Http4s API)
├── Cloud Run Service: consultant-client-app (Nuxt.js Client App)
├── Cloud Run Service: consultant-admin-app (Nuxt.js Admin App)
├── Cloud Run Service: consultant-specialist-app (Nuxt.js Specialist App)
└── Cloud SQL: consultant-postgres-[environment] (PostgreSQL 16)
```

### Network Architecture
- **Public Access**: All Cloud Run services are publicly accessible via HTTPS
- **Private Database Access**: Cloud SQL uses private IP with VPC Connector
- **Load Balancing**: Automatic load balancing across service instances
- **Auto-scaling**: Services automatically scale based on demand

## HTTPS Architecture

### Automatic HTTPS Features

All Cloud Run services automatically get HTTPS with the following features:

- **Google-managed SSL certificates**: Automatically provisioned and renewed
- **HTTP/2 Support**: Enabled by default for better performance
- **Automatic HTTP to HTTPS redirect**: Cloud Run redirects HTTP to HTTPS
- **Security Headers**: Modern security headers configured
- **TLS 1.2 & 1.3**: Latest secure protocols enabled

### Custom Domain HTTPS

When using custom domains:
- SSL certificates are automatically provisioned via Google-managed certificates
- Certificate auto-renewal happens automatically
- Domain verification required before certificate issuance
- DNS records provided for configuration

## Prerequisites

### 1. Required Tools
- **Google Cloud SDK** (`gcloud`) - [Installation Guide](https://cloud.google.com/sdk/docs/install)
- **Docker** - [Installation Guide](https://docs.docker.com/get-docker/)
- **Git** - For cloning the repository
- **OpenSSL** - For generating secrets (usually pre-installed)

### 2. GCP Account Requirements
- GCP account with billing enabled
- Permissions to create projects and manage resources
- Billing account ID (for linking to projects)

### 3. Domain Requirements (Optional)
- Registered domain name (for custom domains)
- Access to DNS configuration (for domain verification)

### 4. Repository Setup
```bash
# Clone the repository
git clone https://github.com/lvn2000/consultant.git
cd consultant/backend

# Make deployment script executable
chmod +x deploy-gcp.sh
```

## Quick Start Deployment

### Development Environment (Auto HTTPS)
```bash
# Deploy to development environment with automatic HTTPS
./deploy-gcp.sh dev

# Or with custom project ID
GCP_PROJECT_ID=your-project-dev ./deploy-gcp.sh dev
```

### Production with Custom Domains
```bash
# Deploy to production with custom domains
./deploy-gcp.sh prod --domain=yourdomain.com

# Or specify individual domains
./deploy-gcp.sh prod \
  --backend-domain=api.yourdomain.com \
  --client-domain=app.yourdomain.com \
  --admin-domain=admin.yourdomain.com \
  --specialist-domain=specialist.yourdomain.com
```

### Advanced Options
```bash
# Disable HTTPS enforcement (not recommended for production)
./deploy-gcp.sh staging --no-https

# Skip custom domain setup
./deploy-gcp.sh prod --skip-domains

# Show help with all options
./deploy-gcp.sh --help
```

## Detailed Deployment Process

### Step 1: Authentication
```bash
# Login to GCP
gcloud auth login

# List available projects
gcloud projects list

# Set default project (optional)
gcloud config set project YOUR_PROJECT_ID
```

### Step 2: Run Deployment Script
The deployment script automates all setup including HTTPS configuration:

```bash
./deploy-gcp.sh [environment] [options]
```

Where `[environment]` can be:
- `dev` - Development environment (db-f1-micro, minimal resources)
- `staging` - Staging environment (db-g1-small, moderate resources)
- `prod` - Production environment (db-custom-2-7680, full resources)

### Step 3: Script Interactive Prompts
The script will prompt for:
1. **GCP Project ID** - Either use existing or create new
2. **Billing Account** - If `GCP_BILLING_ACCOUNT` not set
3. **Confirmation** - Review and confirm deployment

## What the Script Creates

### 1. GCP Project Setup
- Creates or reuses GCP project
- Enables required APIs (Cloud Run, Cloud SQL, Artifact Registry, etc.)
- Links billing account
- Enables domain management APIs for custom domains

### 2. Infrastructure Setup
- **Artifact Registry** - Docker image repository
- **Cloud SQL** - PostgreSQL 16 database with automatic backups
- **VPC Connector** - Secure private database access
- **Secret Manager** - Secure storage for credentials

### 3. Application Deployment with HTTPS
- **Backend API** - Scala application with JVM optimizations and HTTPS
- **Frontend Apps** - Three Nuxt.js applications with HTTPS
- **Auto-scaling** - Services scale from 1-10 instances based on load
- **HTTPS Enforcement** - All services accessible only via HTTPS (configurable)

### 4. Security Configuration
- Randomly generated passwords and secrets
- Secrets stored in Secret Manager
- Private database access via VPC Connector
- HTTPS-only communication by default
- Security headers and secure cookies

## HTTPS Configuration Details

### Automatic HTTPS Features
1. **SSL Certificates**: Google-managed certificates automatically provisioned
2. **HTTP/2**: Enabled for better performance and security
3. **Security Headers**: Modern headers like HSTS, X-Content-Type-Options, etc.
4. **TLS Configuration**: TLS 1.2 and 1.3 with secure ciphers
5. **Certificate Renewal**: Automatic, no manual intervention needed

### Environment Variables for HTTPS
The script configures these environment variables:

```bash
FORCE_HTTPS=true                 # Enforce HTTPS-only access
SECURE_COOKIES=true             # Cookies only sent over HTTPS
SESSION_SECURE=true             # Sessions require HTTPS
ENABLE_HTTP2=true              # Enable HTTP/2 protocol
SECURITY_HEADERS=true          # Add security headers to responses
```

### Custom Domain Setup Process
1. Domain verification (if using Google Domains)
2. DNS record configuration (A/AAAA and CNAME records)
3. SSL certificate provisioning (automatic, takes up to 30 minutes)
4. Domain mapping to Cloud Run services

## Environment-Specific Configuration

### Development (`dev`)
```yaml
Region: us-central1
Database Tier: db-f1-micro (shared CPU, 0.6GB RAM)
Service Scaling: 1-3 instances
HTTPS: Enabled with *.run.app domain
Custom Domains: Optional
Resources: Minimal for testing
```

### Staging (`staging`)
```yaml
Region: us-central1
Database Tier: db-g1-small (shared CPU, 1.7GB RAM)
Service Scaling: 1-5 instances
HTTPS: Enabled, can use custom domains
Custom Domains: Recommended for testing
Resources: Production-like, lower scale
```

### Production (`prod`)
```yaml
Region: us-central1
Database Tier: db-custom-2-7680 (2 vCPU, 7.5GB RAM)
Service Scaling: 3-10 instances
HTTPS: Required with custom domains
Custom Domains: Required for production
Resources: High availability, auto-scaling
```

## Database Configuration

### Cloud SQL Instance
- **Version**: PostgreSQL 16
- **Backups**: Automatic daily at 02:00 UTC
- **Maintenance**: Mondays at 03:00 UTC
- **Storage**: 100GB SSD
- **High Availability**: Zonal (single zone for cost efficiency)

### Database Schema
The database includes:
- Full schema from Flyway migrations
- Test users and data (development only)
- Encrypted sensitive data
- Proper indexes and constraints

### Connection Security
- **Private IP**: Recommended for production (via VPC Connector)
- **Public IP**: Available for development access
- **SSL/TLS**: Enforced for all connections
- **IAM Authentication**: Enabled (can be configured)

## Service URLs and Access

### Default HTTPS URLs (Cloud Run Domains)
```
Backend API:     https://consultant-backend-[hash]-uc.a.run.app
Client App:      https://consultant-client-app-[hash]-uc.a.run.app
Admin App:       https://consultant-admin-app-[hash]-uc.a.run.app
Specialist App:  https://consultant-specialist-app-[hash]-uc.a.run.app
```

### Custom Domain URLs (When Configured)
```
Backend API:     https://api.yourdomain.com
Client App:      https://app.yourdomain.com
Admin App:       https://admin.yourdomain.com
Specialist App:  https://specialist.yourdomain.com
```

### API Endpoints
- `GET /health` - Service health check (HTTPS only)
- `GET /docs` - Swagger/OpenAPI documentation (HTTPS only)
- `POST /api/auth/login` - User authentication (HTTPS only)
- Various CRUD endpoints under `/api/` (HTTPS only)

### Default Test Users (Development Only)
```
User:        user / user
Admin:       admin / admin
Specialist:  spec / spec
```

**WARNING**: Change default credentials immediately in production!

## HTTPS Verification and Testing

### Verify HTTPS Configuration
```bash
# Test HTTPS endpoint
curl -k https://consultant-backend-[hash]-uc.a.run.app/health

# Check SSL certificate details
openssl s_client -connect api.yourdomain.com:443 -servername api.yourdomain.com | openssl x509 -noout -dates

# Test security headers
curl -I https://consultant-backend-[hash]-uc.a.run.app

# Verify HTTP redirects to HTTPS (if FORCE_HTTPS=true)
curl -I http://consultant-backend-[hash]-uc.a.run.app
```

### Expected Security Headers
```
Strict-Transport-Security: max-age=31536000
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
```

## Database Migrations

### Automatic Migration Setup
The deployment script prepares for migrations but doesn't run them automatically for safety.

### Manual Migration Options

#### Option 1: Cloud Run Job (HTTPS enabled)
```bash
# Create migration job with HTTPS access
gcloud run jobs create consultant-migrations \
  --image=REGION-docker.pkg.dev/PROJECT/consultant/consultant-backend:latest \
  --set-env-vars="DB_URL=..." \
  --command="/app/migrate.sh" \
  --region=us-central1
```

#### Option 2: Direct Database Access
```bash
# Connect to Cloud SQL with SSL
gcloud sql connect consultant-postgres-prod --user=postgres

# Run migrations manually
\c consultant
-- Run SQL from data/src/main/resources/db/migration/
```

#### Option 3: Local Migration
```bash
# Build and run migrations locally
cd backend
./run.sh  # This automatically runs Flyway migrations
```

## Monitoring and Logging

### Cloud Console Monitoring
- **Cloud Run**: View request volumes, latencies, errors
- **Cloud SQL**: Monitor database performance, connections
- **Logging**: Structured logs with severity levels
- **SSL Certificates**: Monitor certificate status and expiration

### Health Checks
All services include health endpoints accessible via HTTPS:
```bash
# Test backend health via HTTPS
curl https://consultant-backend-[hash]-uc.a.run.app/health

# Test frontend apps via HTTPS
curl -I https://consultant-client-app-[hash]-uc.a.run.app
```

### Log Access
```bash
# View backend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=consultant-backend" --limit=10

# View SSL certificate events
gcloud logging read "protoPayload.methodName=google.cloud.security.privateca.v1.CertificateAuthorityService.IssueCertificate" --limit=10

# View specific error logs
gcloud logging read "severity>=ERROR" --limit=20
```

## Scaling and Performance

### Auto-scaling Configuration
```
Backend API:    1-10 instances, 80 concurrent requests
Frontend Apps:  1-5 instances, 80 concurrent requests
Database:       Configured based on environment tier
```

### Resource Limits
```yaml
Backend:
  CPU: 1 vCPU
  Memory: 1GB
  Timeout: 300 seconds
  HTTPS: Enabled with HTTP/2

Frontend:
  CPU: 1 vCPU
  Memory: 512MB
  Timeout: 60 seconds
  HTTPS: Enabled with HTTP/2
```

### Performance Optimization with HTTPS
- **HTTP/2**: Multiplexing reduces connection overhead
- **TLS 1.3**: Faster handshake than TLS 1.2
- **Connection Pooling**: 32 database connections
- **CDN**: Frontend assets served efficiently via HTTPS

## Security Best Practices

### 1. HTTPS Security
```bash
# Enforce HTTPS-only access (already done by script)
# Verify no HTTP access
curl -I http://your-service.run.app | grep -i "location\|301\|302"

# Check TLS configuration
nmap --script ssl-enum-ciphers -p 443 api.yourdomain.com
```

### 2. Secrets Management
```bash
# Rotate secrets
gcloud secrets versions add consultant-db-password-prod --data-file=<(openssl rand -base64 32)

# Review secret access
gcloud secrets get-iam-policy consultant-jwt-secret-prod
```

### 3. IAM Configuration
```bash
# Least privilege principle
gcloud run services add-iam-policy-binding consultant-backend \
  --member='serviceAccount:consultant-sa@PROJECT.iam.gserviceaccount.com' \
  --role='roles/run.invoker'
```

### 4. Network Security
- Enable VPC Service Controls
- Configure private database IP
- Set up Cloud Armor for DDoS protection
- Use VPC Connector for private communication
- HTTPS-only external access

### 5. SSL Certificate Management
- Monitor certificate expiration
- Set up alerts for certificate renewal failures
- Regular security scans
- Backup verification

## Cost Management

### Estimated Monthly Costs (Production)

| Resource | Tier | Estimated Cost |
|----------|------|----------------|
| Cloud SQL | db-custom-2-7680 | ~$200/month |
| Cloud Run | 3-10 instances | ~$50-150/month |
| Artifact Registry | Storage | ~$5/month |
| Network Egress | 100GB | ~$10/month |
| SSL Certificates | Google-managed | $0/month |
| **Total** | | **~$265-365/month** |

### Cost Optimization Tips
1. **Development**: Use shared CPU instances
2. **Staging**: Schedule automatic shutdown overnight
3. **Production**: Use committed use discounts
4. **Monitoring**: Set up billing alerts
5. **HTTPS**: Google-managed certificates are free

## Troubleshooting

### Common HTTPS Issues

#### 1. SSL Certificate Not Provisioned
```bash
# Check domain mapping status
gcloud run domain-mappings describe --domain=api.yourdomain.com --region=us-central1

# Check certificate provisioning
gcloud compute ssl-certificates list --global

# Verify DNS configuration
dig api.yourdomain.com
nslookup api.yourdomain.com
```

#### 2. HTTPS Connection Errors
```bash
# Test TLS connectivity
openssl s_client -connect api.yourdomain.com:443 -servername api.yourdomain.com

# Check service URL
gcloud run services describe consultant-backend --region=us-central1 --format="value(status.url)"

# Verify service is accessible
curl -v https://consultant-backend-[hash]-uc.a.run.app/health
```

#### 3. Custom Domain Not Working
```bash
# Check domain verification
gcloud domains verify api.yourdomain.com

# Check DNS records
gcloud run domain-mappings describe --domain=api.yourdomain.com --region=us-central1 --format="value(status.resourceRecords)"

# Test DNS propagation
dig +trace api.yourdomain.com
```

#### 4. HTTP Not Redirecting to HTTPS
```bash
# Check FORCE_HTTPS environment variable
gcloud run services describe consultant-backend --region=us-central1 --format="yaml(spec.template.spec.containers[0].env)"

# Test redirect
curl -I http://consultant-backend-[hash]-uc.a.run.app
```

### Debug Commands
```bash
# Get all service URLs with HTTPS
gcloud run services list --region=us-central1 --format="table[box](NAME,URL)"

# Check service health via HTTPS
for service in consultant-backend consultant-client-app consultant-admin-app consultant-specialist-app; do
  echo "Checking $service HTTPS health..."
  curl -s https://$(gcloud run services describe $service --region=us-central1 --format="value(status.url)")/health
done

# View recent errors
gcloud logging read "severity>=ERROR" --project=PROJECT_ID --limit=20

# Check SSL certificate status
gcloud compute ssl-certificates describe $(gcloud compute ssl-certificates list --filter="name~api.yourdomain.com" --format="value(name)") --global
```

## Maintenance

### Regular Maintenance Tasks

#### Weekly
- Review logs for errors and anomalies
- Check database backup completion
- Monitor costs and usage trends
- Verify SSL certificate status

#### Monthly
- Rotate secrets and certificates
- Review IAM permissions
- Update to latest base images
- Test backup restoration
- Check certificate expiration dates

#### Quarterly
- Review and update security policies
- Performance tuning and optimization
- Capacity planning review
- SSL/TLS configuration review

### SSL Certificate Management
- Google-managed certificates auto-renew
- Monitor for renewal failures
- Set up alerts for expiring certificates
- Test certificate chain validity

### Backup and Recovery

#### Database Backups
- Automatic daily backups
- 7-day retention (configurable)
- Point-in-time recovery
- Encrypted with Google-managed keys

#### Manual Backup
```bash
# Export database with SSL
gcloud sql export sql consultant-postgres-prod gs://BUCKET/backup-$(date +%Y%m%d).sql \
  --database=consultant

# Import database with SSL
gcloud sql import sql consultant-postgres-prod gs://BUCKET/backup.sql \
  --database=consultant
```

#### Disaster Recovery
1. Database: Use Cloud SQL high availability or replicas
2. Application: Redeploy from source control
3. Data: Regular exports to Cloud Storage
4. SSL Certificates: Automatic re-provisioning

## Advanced HTTPS Configuration

### Custom SSL Certificates
If you need to use your own SSL certificates:

```bash
# Upload custom certificate
gcloud compute ssl-certificates create custom-cert \
  --certificate=path/to/certificate.crt \
  --private-key=path/to/private.key

# Use with load balancer (advanced setup)
```

### HTTP Strict Transport Security (HSTS)
- Already configured with 1-year max-age
- Can be extended for preloading
- Requires careful planning for domain changes

### Security Headers Customization
The deployment script sets optimal security headers. To customize:

```bash
# Deploy with custom headers
gcloud run services update consultant-backend \
  --set-env-vars="ADDITIONAL_SECURITY_HEADERS=true" \
  --region=us-central1
```

## CI/CD Integration with HTTPS

### GitHub Actions Example
```yaml
# .github/workflows/deploy-gcp.yml
name: Deploy to GCP with HTTPS

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - id: 'auth'
        uses: 'google-github-actions/auth@v2'
        with:
          credentials_json: '${{ secrets.GCP_SA_KEY }}'
          
      - name: Deploy to GCP with HTTPS
        run: |
          cd backend
          chmod +x deploy-gcp.sh
          ./deploy-gcp.sh prod --domain=${{ vars.PRODUCTION_DOMAIN }}
          
      - name: Verify HTTPS
        run: |
          curl -f https://${{ vars.PRODUCTION_DOMAIN }}/health
```

## Support and Resources

### Documentation
- [Consultant Wiki](https://github.com/lvn2000/consultant-wiki)
- [GCP Cloud Run Documentation](https://cloud.google.com/run/docs)
- [GCP Cloud SQL Documentation](https://cloud.google.com/sql/docs/postgres)
- [GCP SSL Certificates Documentation](https://cloud.google.com/load-balancing/docs/ssl-certificates)

### Troubleshooting Resources
- [GCP Status Dashboard](https://status.cloud.google.com/)
- [Cloud Run Troubleshooting](https://cloud.google.com/run/docs/troubleshooting)
- [SSL Certificate Troubleshooting](https://cloud.google.com/load-balancing/docs/ssl-certificates/troubleshooting)
- [Stack Overflow - google-cloud-platform](https://stackoverflow.com/questions/tagged/google-cloud-platform)

### Getting Help
1. Check deployment logs: `gcloud logging read --limit=50`
2. Review service status: `gcloud run services describe SERVICE_NAME`
3. Check SSL certificates: `gcloud compute ssl-certificates list`
4. Contact support via GCP Console

---

**Last Updated**: 2024-12-15

**Maintainer**: Platform Engineering Team

**Note**: Always test deployments in staging environment before production. Monitor costs and set up billing alerts to avoid unexpected charges. HTTPS is enabled by default and recommended for all environments.