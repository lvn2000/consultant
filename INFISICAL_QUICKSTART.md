# 🚀 Infisical Quick Start

Minimal guide to get started with Infisical in 5 minutes.

## 1. Install CLI

### macOS

```bash
brew install infisical/get-cli/infisical
```

### Linux

```bash
curl -1sLf 'https://dl.cloudsmith.io/public/infisical/infisical-cli/setup.deb.sh' | sudo -E bash
sudo apt-get update && sudo apt-get install -y infisical
```

### Verify

```bash
infisical --version
```

## 2. Create Account

```bash
# Open registration
open https://app.infisical.com/signup

# Or self-hosted
docker-compose -f docker-compose.infisical-server.yml up -d
```

**In UI create:**

1. Organization: "Consultant"
2. Project: "consultant-backend"
3. Environments: `development`, `staging`, `production`

## 3. Authentication

```bash
infisical login
```

## 4. Project Initialization

```bash
cd /home/lvn/prg/scala/Consultant/backend
infisical init

# Select:
# - Organization: Consultant
# - Project: consultant-backend
# - Environment: development
```

## 5. Migrate Existing Secrets

```bash
# Automatic migration
./scripts/setup-infisical-secrets.sh

# Or manually from .env file
./scripts/migrate-to-infisical.sh development .env.security.example
```

## 6. Testing

```bash
# Local run with secrets from Infisical
infisical run --env=development -- sbt compile
infisical run --env=development -- sbt run

# Verify secrets are loaded
infisical secrets list --env=development
```

## 7. Docker Compose

```bash
# 1. Get service token
infisical service-token --env=production

# 2. Create .env.infisical
cat > .env.infisical << EOF
INFISICAL_TOKEN=st.prod.your-token-here
INFISICAL_PROJECT_ID=your-project-id
INFISICAL_ENVIRONMENT=production
EOF

# 3. Start
docker-compose -f docker-compose.infisical.yml --env-file .env.infisical up -d
```

## 8. Kubernetes

```bash
# 1. Install Operator
helm repo add infisical https://infisical.com/helm-charts
helm install infisical-operator infisical/infisical-secrets-operator \
  --namespace infisical-operator-system --create-namespace

# 2. Create auth secret
kubectl create secret generic infisical-auth \
  --from-literal=token='st.prod.xxxx.yyyy.zzzz' \
  --namespace consultant-backend

# 3. Apply configuration
kubectl apply -f kubernetes/infisical-secret.yaml
```

## 9. CI/CD Integration

```bash
# GitHub Actions
- name: Run with Infisical
  run: |
    infisical run --env=production -- sbt assembly
  env:
    INFISICAL_TOKEN: ${{ secrets.INFISICAL_TOKEN }}
```

## ✅ Done

Full documentation: [INFISICAL_SETUP.md](INFISICAL_SETUP.md)

## 🆘 Troubleshooting

**Issue:** "Token expired"

```bash
infisical login  # Re-login
```

**Issue:** "Secret not found"

```bash
infisical secrets list --env=development  # Check list
infisical secrets set KEY_NAME "value" --env=development  # Add it
```

**Issue:** Docker doesn't see secrets

```bash
# Check agent logs
docker logs infisical-agent-1

# Verify .env.infisical was created
cat .env.infisical
```

## 📚 Useful Commands

```bash
# List secrets
infisical secrets list --env=production

# Add secret
infisical secrets set KEY "value" --env=production

# Export secrets
infisical export --env=development --format=dotenv

# Create service token
infisical service-token --env=production

# Check current user
infisical whoami
```

## 🔒 Production Checklist

- [ ] Rotate all secrets after migration
- [ ] MFA enabled for all team members
- [ ] Audit webhooks configured
- [ ] RBAC configured (developers: dev/staging, devops: all)
- [ ] Backup configured
- [ ] Local .env files removed
