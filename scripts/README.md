# Development Scripts

This folder contains scripts for development, testing, and maintenance tasks.

## 🧪 Testing Scripts

These scripts are used for quick manual testing during development.

| Script | Description |
|--------|-------------|
| [`test-admin-count.sh`](test-admin-count.sh) | Test admin count endpoint with authentication |
| [`test-slots.sh`](test-slots.sh) | Test specialist availability slots endpoint |
| [`test-specialists-rates.sh`](test-specialists-rates.sh) | Test specialists search with category rates |
| [`test-specialists-response.sh`](test-specialists-response.sh) | Test specialists endpoint response structure |

### Usage

All test scripts assume the API is running on `http://localhost:8090`.

```bash
# Make scripts executable (first time only)
chmod +x scripts/test-*.sh

# Run individual test scripts
./scripts/test-admin-count.sh
./scripts/test-slots.sh
./scripts/test-specialists-rates.sh
./scripts/test-specialists-response.sh
```

**Requirements:**
- API server running (`./run.sh`)
- `jq` installed for JSON parsing
- Test credentials in database (see Consultant Wiki)

## 🔧 Setup & Configuration Scripts

| Script | Description |
|--------|-------------|
| [`generate-ssl-certificates.sh`](generate-ssl-certificates.sh) | Generate self-signed SSL certificates for HTTPS |
| [`setup-infisical-secrets.sh`](setup-infisical-secrets.sh) | Setup Infisical secrets management |
| [`migrate-to-infisical.sh`](migrate-to-infisical.sh) | Migrate from .env to Infisical secrets |

### SSL Certificate Generation

```bash
# Generate development certificates
./scripts/generate-ssl-certificates.sh

# Generate production certificates (Let's Encrypt)
./scripts/generate-ssl-certificates.sh -p
```

See Consultant Wiki for HTTPS configuration details.

### Infisical Setup

```bash
# Setup Infisical secrets
./scripts/setup-infisical-secrets.sh

# Migrate existing .env to Infisical
./scripts/migrate-to-infisical.sh
```

See Consultant Wiki for Infisical configuration details.

## 🚀 Main Startup Scripts (Root Folder)

These scripts remain in the root folder for easy access:

| Script | Description |
|--------|-------------|
| [`run.sh`](../run.sh) | **Main startup script** - Starts API with PostgreSQL |
| [`start-https.sh`](../start-https.sh) | Start full stack with HTTPS enabled |

## 📝 Adding New Scripts

When adding new development scripts:

1. Use descriptive names (e.g., `test-*.sh`, `setup-*.sh`)
2. Add comments at the top explaining purpose
3. Document in this README
4. Make executable: `chmod +x scripts/your-script.sh`

## 🛠️ Script Development Tips

```bash
# Debug mode - print each command
bash -x scripts/test-admin-count.sh

# Check for syntax errors
bash -n scripts/your-script.sh

# View required dependencies
head -20 scripts/your-script.sh
```
