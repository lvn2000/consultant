# Security Quick Start

## 🔐 What's Added

### 1. Authentication

- **JWT tokens** (Access + Refresh)
- **OIDC (Keycloak)** optional during migration
- **Password hashing** (PBKDF2 with 210k iterations)
- **Account lockout** (5 attempts, 15 min lockout)

### 2. Authorization

- **RBAC**: Client, Specialist, Admin roles
- **Permissions**: Granular access rights
- **Middleware**: Endpoint protection

### 3. Data Protection

- **DB encryption** (pgcrypto for PII)
- **Password requirements** (8+ chars, special symbols)
- **Audit logging** (all security actions)

### 4. API Endpoints

#### Public

```bash
POST /api/auth/register  # Registration (legacy)
POST /api/auth/login     # Login (legacy)
POST /api/auth/refresh   # Token refresh (legacy)
```

#### Protected (JWT required)

```bash
POST /api/auth/logout    # Logout (legacy)
GET  /api/users      # With Bearer token
```

## 🚀 Usage

### 1. Registration

```bash
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "role": "client"
  }'
```

**Response:**

```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "uuid-token",
  "expiresAt": "2026-01-13T20:15:00Z",
  "userId": "...",
  "email": "user@example.com",
  "role": "Client"
}
```

### 2. Login

```bash
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

### 3. Protected Request

```bash
curl -X GET http://localhost/api/users \
  -H "Authorization: Bearer eyJhbGc..."
```

### 4. Token Refresh (every 15 minutes)

```bash
curl -X POST http://localhost/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "uuid-from-login"
  }'
```

## 🔧 Setup

### 1. Environment Variables

Copy `.env.security.example` to `.env`:

```bash
cp .env.security.example .env
```

### 2. Generate JWT Secret

```bash
# Linux/Mac
openssl rand -base64 64

# Or in Scala
scala> import java.security.SecureRandom
scala> import java.util.Base64
scala> val random = new SecureRandom()
scala> val bytes = new Array[Byte](64)
scala> random.nextBytes(bytes)
scala> Base64.getEncoder.encodeToString(bytes)
```

### 3. OIDC (optional)

```bash
export OIDC_ENABLED=true
export OIDC_ISSUER="https://auth.example.com/realms/consultant"
export OIDC_JWKS_URI="https://auth.example.com/realms/consultant/protocol/openid-connect/certs"
export OIDC_AUDIENCE="consultant-web"
export OIDC_ALLOWED_ALGS="RS256"
export OIDC_JWKS_CACHE_SECONDS=600
export LEGACY_AUTH_ENABLED=true
```

### 4. DB Migration

```bash
# Apply V001__baseline_schema.sql
docker-compose exec postgres-master psql -U consultant_user -d consultant -f /docker-entrypoint-initdb.d/V001__baseline_schema.sql
```

### 5. HTTPS in Production

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.3 TLSv1.2;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
}
```

## 🛡️ Security Features

✅ **JWT Authentication** - Stateless tokens  
✅ **Password Hashing** - PBKDF2 210k iterations  
✅ **Account Lockout** - Brute force protection  
✅ **Audit Logging** - All security events  
✅ **DB Encryption** - pgcrypto for PII  
✅ **RBAC** - Role-Based Access Control  
✅ **Rate Limiting** - nginx DDoS protection  
✅ **HTTPS/TLS** - Encryption in transit  
✅ **CORS** - Cross-Origin protection  
✅ **Security Headers** - XSS, Clickjacking protection  

## 📊 DB Tables

- `credentials` - Hashed passwords + roles
- `refresh_tokens` - Refresh tokens for renewal
- `security_audit_log` - Audit of all actions
- `user_sessions` - Active sessions (Redis)

## 🔒 Production Checklist

- [ ] Generate strong JWT_SECRET (64+ chars)
- [ ] Configure OIDC env vars (if enabled)
- [ ] Configure HTTPS with valid certificate
- [ ] Enable HSTS headers
- [ ] Configure CORS for your frontend
- [ ] Enable rate limiting
- [ ] Configure AWS Secrets Manager for secrets
- [ ] Enable DB encryption at rest (RDS)
- [ ] Configure CloudWatch alerts for suspicious activity
- [ ] Regular security audits
- [ ] Encrypted backups

Details in [SECURITY.md](./SECURITY.md) 🔐
