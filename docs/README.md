# Consultant Backend - Documentation Index

This folder contains all documentation for the Consultant Backend system.

## 📚 Documentation Categories

### 🚀 Getting Started

| Document | Description |
|----------|-------------|
| [DATABASE_QUICK_START.md](DATABASE_QUICK_START.md) | Quick start guide for database setup |
| [HTTPS_QUICKSTART.md](HTTPS_QUICKSTART.md) | HTTPS setup quick start |
| [INFISICAL_QUICKSTART.md](INFISICAL_QUICKSTART.md) | Infisical secrets management quick start |
| [SCALING_QUICKSTART.md](SCALING_QUICKSTART.md) | Quick scaling guide |
| [SECURITY_QUICKSTART.md](SECURITY_QUICKSTART.md) | Security features quick start |

### 📖 Architecture & Design

| Document | Description |
|----------|-------------|
| [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md) | **Mermaid diagrams** - Flowcharts, Sequence, Class, ER, State diagrams |
| [SCALING.md](SCALING.md) | Detailed scaling strategies |
| [SECURITY.md](SECURITY.md) | Security architecture and implementation |

### 💾 Database

| Document | Description |
|----------|-------------|
| [DATABASE_CONFIG.md](DATABASE_CONFIG.md) | Database configuration details |
| [DATABASE_QUICK_START.md](DATABASE_QUICK_START.md) | Database quick start guide |

### 🔧 Deployment

| Document | Description |
|----------|-------------|
| [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) | Docker deployment guide |
| [INFISICAL_SETUP.md](INFISICAL_SETUP.md) | Infisical setup guide |
| [HTTPS_QUICKSTART.md](HTTPS_QUICKSTART.md) | HTTPS configuration |

### 🔐 Security

| Document | Description |
|----------|-------------|
| [SECURITY.md](SECURITY.md) | Security documentation |
| [SECURITY_QUICKSTART.md](SECURITY_QUICKSTART.md) | Security quick start |
| [TEST_CREDENTIALS.md](TEST_CREDENTIALS.md) | Test credentials and accounts |

### 📡 API Reference

| Document | Description |
|----------|-------------|
| [NOTIFICATIONS_API_REFERENCE.md](NOTIFICATIONS_API_REFERENCE.md) | Notifications API reference |
| [NOTIFICATIONS_PREFERENCES_API.md](NOTIFICATIONS_PREFERENCES_API.md) | Notification preferences API |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | Quick API reference |

### 🎯 Features

| Document | Description |
|----------|-------------|
| [AVAILABILITY_FEATURE.md](AVAILABILITY_FEATURE.md) | Availability management feature |
| [CLIENT_APP_INTEGRATION.md](CLIENT_APP_INTEGRATION.md) | Client app integration guide |
| [CONNECTIONS_IMPLEMENTATION.md](CONNECTIONS_IMPLEMENTATION.md) | Connections feature implementation |
| [CONSULTATION_NOTIFICATIONS.md](CONSULTATION_NOTIFICATIONS.md) | Consultation notifications system |

## 📂 Project Structure

```
backend/
├── docs/                      # 📚 All documentation (this folder)
│   ├── README.md             # This index file
│   ├── ARCHITECTURE_DIAGRAMS.md
│   ├── DATABASE_*.md
│   ├── SECURITY_*.md
│   └── ...
├── scripts/                   # 🛠️ Development & test scripts
│   ├── README.md             # Scripts documentation
│   ├── test-*.sh             # API test scripts
│   ├── generate-ssl-certificates.sh
│   └── setup-infisical-*.sh
├── api/                       # HTTP API layer (Tapir, Http4s)
├── core/                      # Domain models & business logic
├── data/                      # PostgreSQL repositories (Doobie)
├── infrastructure/            # AWS/Local adapters
├── client-app/                # Nuxt.js client frontend
├── admin-app/                 # Nuxt.js admin frontend
└── specialist-app/            # Nuxt.js specialist frontend
```

## 🔗 Main README

For general project overview and quick start, see the main [README.md](../README.md) in the root directory.

## 📝 Contributing

When adding new features or making changes:
1. Update relevant documentation in this folder
2. Keep documentation files focused on single topics
3. Use clear, descriptive filenames
4. Update this index if adding new categories
