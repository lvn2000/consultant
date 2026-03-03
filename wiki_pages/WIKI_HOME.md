# Consultant Backend - GitHub Wiki

Welcome to the Consultant Backend GitHub Wiki! This is the central documentation hub for the Consultant application - a Scala-based backend system for connecting clients with specialists.

## 📚 Table of Contents

### 🚀 Getting Started
- [Quick Start Guide](Quick-Start-Guide)
- [Database Setup](Database-Setup)
- [HTTPS Configuration](HTTPS-Configuration)
- [Security Features](Security-Features)

### 🏗️ Architecture & Design
- [System Architecture](System-Architecture)
- [Project Structure](Project-Structure)
- [Hexagonal Architecture](Hexagonal-Architecture)
- [Scaling Strategies](Scaling-Strategies)

### 🛠️ Development
- [Development Setup](Development-Setup)
- [API Reference](API-Reference)
- [Database Configuration](Database-Configuration)
- [Testing Guide](Testing-Guide)

### 🚀 Deployment
- [Docker Deployment](Docker-Deployment)
- [Production HTTPS Deployment](Production-HTTPS-Deployment)
- [Infisical Setup](Infisical-Setup)

### 🔐 Security
- [Security Architecture](Security-Architecture)
- [Security Best Practices](Security-Best-Practices)

### 🎯 Features
- [Availability Management](Availability-Management)
- [Client App Integration](Client-App-Integration)
- [Connections Feature](Connections-Feature)
- [Consultation Notifications](Consultation-Notifications)
- [Notification Preferences](Notification-Preferences)

### 📊 API Documentation
- [Notifications API](Notifications-API)
- [Quick API Reference](Quick-API-Reference)

### 🤝 Contributing
- [Code Standards](Code-Standards)
- [Pull Request Process](Pull-Request-Process)

---

## 📖 About This Project

The Consultant Backend is built using **Hexagonal Architecture** (Ports & Adapters) to ensure easy migration to AWS:

```
┌─────────────────────────────────────────────────────────┐
│                        API Layer                        │
│  (HTTP endpoints, DTOs, Tapir routes)                  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                     Core Layer                          │
│  (Domain models, Business logic, Port interfaces)      │
└─────────────┬──────────────────────────┬────────────────┘
              │                          │
┌─────────────▼──────────┐   ┌──────────▼──────────────┐
│    Data Layer          │   │  Infrastructure Layer   │
│  (PostgreSQL repos)    │   │  (AWS/Local adapters)   │
└────────────────────────┘   └─────────────────────────┘
```

### Key Features
- **Domain Models:** Users (clients), Specialists with categories/ratings/availability, Consultations with status tracking, Hierarchical categories
- **Search & Matching:** Search specialists by category, rating, price, experience; Consultation request handling; Rating and review system
- **AWS-Ready:** S3 for file storage, SES for emails, SNS for SMS, SQS for async messaging; Easy toggle between local and AWS implementations

### Technology Stack
- Scala 3.3.1
- Cats & Cats Effect - Functional programming
- Http4s - HTTP server
- Tapir - Type-safe API definitions
- Doobie - PostgreSQL access
- Circe - JSON handling
- Ciris - Configuration management
- AWS SDK 2 - AWS services integration
- FS2-AWS - Reactive AWS streaming