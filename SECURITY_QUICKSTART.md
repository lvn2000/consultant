# Security Quick Start

## 🔐 Что добавлено

### 1. Аутентификация

- **JWT токены** (Access + Refresh)
- **Password hashing** (PBKDF2 с 210k итераций)
- **Account lockout** (5 попыток, блокировка 15 мин)

### 2. Авторизация

- **RBAC**: Client, Specialist, Admin роли
- **Permissions**: Детальные права доступа
- **Middleware**: Защита endpoint'ов

### 3. Защита данных

- **DB encryption** (pgcrypto для PII)
- **Password requirements** (8+ символов, спецсимволы)
- **Audit logging** (все действия безопасности)

### 4. API Endpoints

#### Public

```bash
POST /auth/register  # Регистрация
POST /auth/login     # Вход
POST /auth/refresh   # Обновление токена
```

#### Protected (требуется JWT)

```bash
POST /auth/logout    # Выход
GET  /api/users      # С Bearer token
```

## 🚀 Использование

### 1. Регистрация

```bash
curl -X POST http://localhost/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "role": "client"
  }'
```

**Ответ:**

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

### 2. Вход

```bash
curl -X POST http://localhost/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

### 3. Защищенный запрос

```bash
curl -X GET http://localhost/api/users \
  -H "Authorization: Bearer eyJhbGc..."
```

### 4. Обновление токена (каждые 15 минут)

```bash
curl -X POST http://localhost/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "uuid-from-login"
  }'
```

## 🔧 Настройка

### 1. Environment Variables

Скопируйте `.env.security.example` в `.env`:

```bash
cp .env.security.example .env
```

### 2. Сгенерируйте JWT Secret

```bash
# Linux/Mac
openssl rand -base64 64

# Или в Scala
scala> import java.security.SecureRandom
scala> import java.util.Base64
scala> val random = new SecureRandom()
scala> val bytes = new Array[Byte](64)
scala> random.nextBytes(bytes)
scala> Base64.getEncoder.encodeToString(bytes)
```

### 3. БД Migration

```bash
# Применить V002__security_tables.sql
docker-compose exec postgres-master psql -U consultant_user -d consultant -f /docker-entrypoint-initdb.d/V002__security_tables.sql
```

### 4. HTTPS в Production

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

✅ **JWT Authentication** - Stateless токены  
✅ **Password Hashing** - PBKDF2 210k итераций  
✅ **Account Lockout** - Защита от brute force  
✅ **Audit Logging** - Все события безопасности  
✅ **DB Encryption** - pgcrypto для PII  
✅ **RBAC** - Role-Based Access Control  
✅ **Rate Limiting** - nginx защита от DDoS  
✅ **HTTPS/TLS** - Шифрование в транспорте  
✅ **CORS** - Cross-Origin защита  
✅ **Security Headers** - XSS, Clickjacking защита  

## 📊 Таблицы БД

- `credentials` - Хешированные пароли + роли
- `refresh_tokens` - Refresh токены для продления
- `security_audit_log` - Аудит всех действий
- `user_sessions` - Активные сессии (Redis)

## 🔒 Production Checklist

- [ ] Сгенерировать сильный JWT_SECRET (64+ символов)
- [ ] Настроить HTTPS с валидным сертификатом
- [ ] Включить HSTS заголовки
- [ ] Настроить CORS для вашего фронтенда
- [ ] Включить rate limiting
- [ ] Настроить AWS Secrets Manager для секретов
- [ ] Включить DB encryption at rest (RDS)
- [ ] Настроить CloudWatch алерты на подозрительную активность
- [ ] Регулярные security audits
- [ ] Backups с шифрованием

Детали в [SECURITY.md](./SECURITY.md) 🔐
