# Infisical Integration Guide

Complete guide for Infisical integration for secure secrets management in Consultant Backend.

## 📋 Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Local Development](#local-development)
4. [Docker Compose](#docker-compose)
5. [Kubernetes Production](#kubernetes-production)
6. [Migrating Existing Secrets](#migrating-existing-secrets)
7. [Best Practices](#best-practices)

---

## Overview

### What Infisical Replaces

**Before (current approach):**

```bash
# .env files in plain text
JWT_SECRET=dev-jwt-secret-change-in-production
DB_PASSWORD=consultant_pass
DB_ENCRYPTION_KEY=dev-encryption-key
```

**After (Infisical):**

- ✅ Centralized secrets storage
- ✅ End-to-end encryption
- ✅ Automatic rotation
- ✅ Audit logs for all access
- ✅ RBAC for the team
- ✅ Secret version control
- ✅ Git secret scanning

### Integration Architecture

```
┌─────────────────┐
│ Infisical Cloud │ or Self-hosted
│   (encrypted)   │
└────────┬────────┘
         │
    ┌────┴────┐
    │ Infisical│
    │  Agent  │ (sidecar in each pod/container)
    └────┬────┘
         │
    ┌────┴─────┐
    │   App    │ reads from /run/secrets/
    │ Container│ or env variables
    └──────────┘
```

---

## Quick Start

### 1. Create Infisical Account

**Option A: Infisical Cloud (recommended for getting started)**

```bash
# 1. Sign up
open https://app.infisical.com/signup

# 2. Create organization: "Consultant"
# 3. Create project: "consultant-backend"
# 4. Create environments: development, staging, production
```

**Option B: Self-hosted (for production)**

```bash
# Docker Compose for Infisical server
curl -o docker-compose.infisical.yml https://raw.githubusercontent.com/Infisical/infisical/main/docker-compose.prod.yml

docker-compose -f docker-compose.infisical.yml up -d

# Available at http://localhost:8080
```

### 2. Install Infisical CLI

```bash
# macOS
brew install infisical/get-cli/infisical

# Linux
curl -1sLf 'https://dl.cloudsmith.io/public/infisical/infisical-cli/setup.deb.sh' | sudo -E bash
sudo apt-get update && sudo apt-get install -y infisical

# Verify
infisical --version
```

### 3. Authentication

```bash
# Login
infisical login

# Or via service token (for CI/CD)
export INFISICAL_TOKEN="st.xxxx.yyyy.zzzz"
```

### 4. Project Initialization

```bash
cd /home/lvn/prg/scala/Consultant/backend

# Link project
infisical init

# Select:
# - Organization: Consultant
# - Project: consultant-backend
# - Environment: development (for local development)
```

---

## Local Development

### Option 1: Infisical CLI (easiest)

```bash
# Run sbt with automatic secret injection
infisical run --env=development -- sbt run

# Or for compilation
infisical run --env=development -- sbt compile

# For tests
infisical run --env=development -- sbt test
```

### Option 2: Export to shell

```bash
# Export all secrets to current shell
infisical export --env=development --format=dotenv > .env.local

# Then source
source .env.local

# Or inline
eval $(infisical export --env=development --format=shell)

sbt run
```

### Option 3: IntelliJ IDEA / VS Code Integration

**IntelliJ IDEA:**

1. Install Infisical plugin
2. Settings → Infisical → Configure project
3. Run Configuration will automatically load secrets

**VS Code:**

```bash
# Use built-in terminal with infisical run
infisical run --env=development -- code .
```

### Setup Development Secrets

```bash
# Create development secrets
infisical secrets set JWT_SECRET "dev-jwt-secret-for-local-development-min-64-characters-long" --env=development
infisical secrets set DB_HOST "localhost" --env=development
infisical secrets set DB_PORT "5432" --env=development
infisical secrets set DB_NAME "consultant_dev" --env=development
infisical secrets set DB_USER "postgres" --env=development
infisical secrets set DB_PASSWORD "postgres" --env=development
infisical secrets set DB_ENCRYPTION_KEY "dev-encryption-key-change-in-prod" --env=development
infisical secrets set REDIS_HOST "localhost" --env=development
infisical secrets set REDIS_PORT "6379" --env=development
infisical secrets set SESSION_SECRET "dev-session-secret" --env=development
```

---

## Docker Compose

### Infisical Agent Integration

Updated `docker-compose.infisical.yml` has been created in the project root. It includes:

1. **Infisical Agent** as a sidecar for each app instance
2. Automatic secret synchronization
3. Volume mounting for shared secrets

### Running with Infisical

```bash
# 1. Get Service Token for docker-compose
infisical service-token

# 2. Create .env.infisical
cat > .env.infisical << EOF
INFISICAL_TOKEN=st.xxxx.yyyy.zzzz
INFISICAL_PROJECT_ID=your-project-id
EOF

# 3. Start
docker-compose -f docker-compose.infisical.yml --env-file .env.infisical up -d

# 4. Check logs
docker-compose -f docker-compose.infisical.yml logs -f app-1
```

### How It Works

```yaml
services:
  infisical-agent-1:
    image: infisical/cli:latest
    command: agent --exit-on-sync
    environment:
      - INFISICAL_TOKEN=${INFISICAL_TOKEN}
      - INFISICAL_PROJECT_ID=${INFISICAL_PROJECT_ID}
      - INFISICAL_ENVIRONMENT=production
    volumes:
      - secrets-1:/run/secrets
  
  app-1:
    depends_on:
      - infisical-agent-1
    volumes:
      - secrets-1:/run/secrets:ro
    environment:
      # Read from mounted files
      - JWT_SECRET_FILE=/run/secrets/JWT_SECRET
      - DB_PASSWORD_FILE=/run/secrets/DB_PASSWORD
```

---

## Kubernetes Production

### 1. Install Infisical Kubernetes Operator

```bash
# Add Helm repo
helm repo add infisical https://infisical.com/helm-charts
helm repo update

# Install operator
helm install infisical-operator infisical/infisical-secrets-operator \
  --namespace infisical-operator-system \
  --create-namespace
```

### 2. Create Authentication Secret

```bash
# Get service token for production
infisical service-token --env=production

# Create Kubernetes secret
kubectl create secret generic infisical-auth \
  --from-literal=token='st.prod.xxxx.yyyy.zzzz' \
  --namespace consultant-backend
```

### 3. Apply InfisicalSecret CR

```bash
# Apply configuration (created in kubernetes/infisical-secret.yaml)
kubectl apply -f kubernetes/infisical-secret.yaml

# Check
kubectl get infisicalsecrets -n consultant-backend
kubectl describe infisicalsecret consultant-secrets -n consultant-backend
```

### 4. Update Deployment

Deployment automatically uses secrets via `envFrom`:

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        envFrom:
        - secretRef:
            name: consultant-managed-secret  # created by Infisical Operator
```

### 5. Dynamic Secrets for PostgreSQL

```bash
# In Infisical UI:
# 1. Settings → Dynamic Secrets → Add Integration
# 2. Choose: PostgreSQL
# 3. Configure:
#    - Host: your-rds-endpoint.amazonaws.com
#    - Admin credentials
#    - Template for generated users:
#      username: consultant_app_{timestamp}
#      password: {generated}
#      TTL: 1 hour

# Operator will automatically rotate credentials every hour
```

---

## Migrating Existing Secrets

### Automatic Migration from .env

```bash
# Migration script created: scripts/migrate-to-infisical.sh
cd /home/lvn/prg/scala/Consultant/backend
chmod +x scripts/migrate-to-infisical.sh

# Migrate development secrets
./scripts/migrate-to-infisical.sh development .env.security.example

# Migrate production secrets (if you have .env.production)
./scripts/migrate-to-infisical.sh production .env.production
```

### Manual Migration for Critical Secrets

```bash
# Production secrets - set manually with strong passwords
infisical secrets set JWT_SECRET "$(openssl rand -base64 64)" --env=production
infisical secrets set DB_ENCRYPTION_KEY "$(openssl rand -base64 32)" --env=production
infisical secrets set SESSION_SECRET "$(openssl rand -base64 32)" --env=production

# Database credentials (if not using dynamic secrets)
infisical secrets set DB_PASSWORD "$(openssl rand -base64 24)" --env=production
```

### Verify Migration

```bash
# List all secrets
infisical secrets list --env=development
infisical secrets list --env=production

# Compare with .env file
diff <(cat .env.security.example | grep -v '^#' | sort) \
     <(infisical secrets list --env=development --format=dotenv | sort)
```

---

## Best Practices

### 1. Environment Strategy

```
development  → for local development (weak secrets OK)
staging      → production config copy (but separate credentials)
production   → strong secrets, audit logs enabled, strict RBAC
```

### 2. Secret Naming Convention

```bash
# Good
JWT_SECRET
DB_PASSWORD
REDIS_CONNECTION_STRING
AWS_ACCESS_KEY_ID

# Bad (don't use environment prefixes in names)
PROD_JWT_SECRET  # environment is already set via --env flag
DEV_DB_PASSWORD
```

### 3. Rotation Policy

```bash
# Critical secrets
JWT_SECRET:           rotate every 90 days
SESSION_SECRET:       rotate every 90 days
DB_ENCRYPTION_KEY:    rotate once a year (requires data re-encryption!)

# Database credentials
DB_PASSWORD:          dynamic secrets (auto-rotation every hour)

# API keys
AWS_ACCESS_KEY:       rotate every 90 days
THIRD_PARTY_KEYS:     rotate per provider policy
```

### 4. Access Control

```bash
# RBAC in Infisical UI:

# Developers
- read access: development, staging
- no access: production

# DevOps/SRE
- read/write: all environments
- audit logs: enabled

# CI/CD (service tokens)
- read only: specific secrets needed for deployment
- scope: production environment only
- expiration: 90 days
```

### 5. Audit and Monitoring

```bash
# Enable webhook for audit events
# Infisical UI → Settings → Webhooks → Add Webhook
# URL: https://your-monitoring-system.com/webhooks/infisical

# Event types:
- SECRET_READ
- SECRET_CREATED
- SECRET_UPDATED
- SECRET_DELETED
- USER_ACCESS_GRANTED
- USER_ACCESS_REVOKED

# CloudWatch/Grafana alerts for:
- Frequent failed access attempts
- Production secret changes
- New service token creations
```

### 6. Disaster Recovery

```bash
# Regular backup of secrets (encrypted)
infisical secrets list --env=production --format=json > backup-$(date +%Y%m%d).json
openssl enc -aes-256-cbc -salt -in backup-*.json -out backup-*.json.enc

# Store in separate secure location (S3 encrypted bucket)
aws s3 cp backup-*.json.enc s3://your-secrets-backup-bucket/

# Restore procedure
aws s3 cp s3://your-secrets-backup-bucket/backup-latest.json.enc .
openssl enc -aes-256-cbc -d -in backup-*.json.enc -out backup.json
# Import via Infisical UI or API
```

---

## Troubleshooting

### Issue: "Infisical token expired"

```bash
# Re-login
infisical login

# Or create new service token
infisical service-token --env=production
```

### Issue: "Cannot connect to Infisical API"

```bash
# Check connectivity
curl https://app.infisical.com/api/status

# Check firewall rules
# Infisical Cloud requires access to:
# - app.infisical.com:443
# - api.infisical.com:443

# For self-hosted check DNS
nslookup your-infisical-instance.com
```

### Issue: "Secret not found"

```bash
# Check correct environment
infisical secrets list --env=production

# Check permissions
infisical whoami

# Check project ID
cat .infisical.json
```

### Issue: "Kubernetes operator not syncing"

```bash
# Check operator logs
kubectl logs -n infisical-operator-system deployment/infisical-operator

# Check InfisicalSecret status
kubectl describe infisicalsecret consultant-secrets -n consultant-backend

# Check auth secret
kubectl get secret infisical-auth -n consultant-backend -o yaml
```

---

## Migration Checklist

### Pre-migration

- [ ] Created Infisical account/organization
- [ ] Installed Infisical CLI locally
- [ ] Created project "consultant-backend"
- [ ] Created environments: dev, staging, prod
- [ ] Added team members with correct roles

### Development Environment

- [ ] Migrated .env.security.example → Infisical development
- [ ] Tested `infisical run -- sbt compile`
- [ ] Updated README with team instructions
- [ ] Verified local dev environment works

### Docker Compose

- [ ] Created service token for docker-compose
- [ ] Updated docker-compose.infisical.yml
- [ ] Tested startup with Infisical Agent
- [ ] Verified health checks
- [ ] Verified nginx load balancing

### Kubernetes Production

- [ ] Installed Infisical Kubernetes Operator
- [ ] Created production service token
- [ ] Applied InfisicalSecret CR
- [ ] Updated Deployment to use managed secrets
- [ ] Configured dynamic secrets for PostgreSQL (optional)
- [ ] Tested in staging environment
- [ ] Production deployment

### Post-migration

- [ ] Removed old .env files from servers
- [ ] Updated CI/CD pipeline
- [ ] Configured audit webhooks
- [ ] Documented rotation process
- [ ] Trained team on Infisical usage
- [ ] Set up alerts for critical secret changes

### Security Audit

- [ ] Rotated all production secrets after migration
- [ ] Reviewed access controls (RBAC)
- [ ] Enabled MFA for all team members
- [ ] Configured secret expiration policy
- [ ] Checked audit logs for first week
- [ ] Penetration testing after migration

---

## Useful Commands

```bash
# View all secrets
infisical secrets list --env=production

# Add new secret
infisical secrets set KEY_NAME "value" --env=production

# Update existing
infisical secrets update KEY_NAME "new-value" --env=production

# Delete secret
infisical secrets delete KEY_NAME --env=production

# Rollback to previous version
infisical secrets rollback KEY_NAME --version=2 --env=production

# Export for backup
infisical secrets export --env=production --format=json > backup.json

# Create service token for CI/CD
infisical service-token --env=production --scope="read"

# Check current user
infisical whoami

# Check projects
infisical projects list
```

---

## Additional Resources

- [Infisical Documentation](https://infisical.com/docs)
- [Kubernetes Operator Guide](https://infisical.com/docs/integrations/platforms/kubernetes)
- [Docker Integration](https://infisical.com/docs/integrations/platforms/docker-intro)
- [CLI Reference](https://infisical.com/docs/cli/overview)
- [API Reference](https://infisical.com/docs/api-reference/overview/introduction)
- [Security Best Practices](https://infisical.com/docs/internals/security)

## Support

- Community Slack: https://infisical.com/slack
- GitHub Issues: https://github.com/Infisical/infisical/issues
- Email: team@infisical.com

---

**Next Steps:** Start with local development (Section 3), then Docker Compose (Section 4), and finally Kubernetes (Section 5).
