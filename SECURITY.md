# Security Architecture

## 🔐 Компоненты безопасности

### 1. **Аутентификация (Authentication)**

#### JWT Tokens

- **Access Token**: JWT с TTL 15 минут, используется для API запросов
- **Refresh Token**: UUID с TTL 7 дней, хранится в БД для продления сессии
- **Алгоритм**: HS512 (HMAC with SHA-512)

#### Password Security

- **Хеширование**: PBKDF2WithHmacSHA512
- **Итерации**: 210,000 (OWASP 2024+ стандарт)
- **Key length**: 512 бит
- **Salt**: 32 байта криптографически стойкая соль на пользователя

#### Требования к паролю

- Минимум 8 символов
- Минимум 1 строчная буква
- Минимум 1 заглавная буква
- Минимум 1 цифра
- Минимум 1 специальный символ

### 2. **Авторизация (Authorization)**

#### Role-Based Access Control (RBAC)

```scala
enum UserRole:
  case Client       // Обычный клиент
  case Specialist   // Специалист
  case Admin        // Администратор

enum Permission:
  case ReadUser
  case WriteUser
  case DeleteUser
  case ReadSpecialist
  case WriteSpecialist
  case ManageConsultations
  case ManageCategories
  case AdminAccess
```

#### Права доступа

| Роль | Права |
|------|-------|
| **Client** | ReadUser, ManageConsultations |
| **Specialist** | ReadSpecialist, WriteSpecialist, ManageConsultations |
| **Admin** | Все права |

### 3. **Защита от атак**

#### Account Lockout

- **Максимум попыток входа**: 5
- **Время блокировки**: 15 минут
- Счетчик сбрасывается после успешного входа

#### Session Management

- Сессии хранятся в Redis с TTL
- Session ID генерируется криптографически стойким генератором
- Возможность logout со всех устройств

#### Audit Logging

Логируются все события безопасности:

- LOGIN_SUCCESS / LOGIN_FAILED
- LOGIN_BLOCKED
- LOGOUT / LOGOUT_ALL
- REGISTER
- PASSWORD_CHANGE
- Включая: userId, action, IP address, user agent, timestamp

### 4. **Защита данных**

#### В базе данных

```sql
-- Шифрование чувствительных данных через pgcrypto
CREATE EXTENSION pgcrypto;

-- Шифрованные колонки
ALTER TABLE users ADD COLUMN encrypted_ssn BYTEA;  -- SSN
ALTER TABLE users ADD COLUMN encrypted_payment_info BYTEA;  -- Платежные данные

-- Пример шифрования/дешифрования
INSERT INTO users (id, email, encrypted_ssn) 
VALUES (gen_random_uuid(), 'user@example.com', 
        pgp_sym_encrypt('123-45-6789', 'encryption_key'));

SELECT pgp_sym_decrypt(encrypted_ssn, 'encryption_key') FROM users;
```

#### В транспорте

- **HTTPS/TLS 1.3** обязателен для production
- Certificate pinning для критичных запросов
- HSTS (HTTP Strict Transport Security) headers

### 5. **API Security**

#### HTTP Headers

```nginx
# Security headers в nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

#### Rate Limiting

Уже реализовано в nginx:

```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
limit_req zone=api_limit burst=200 nodelay;
limit_conn_zone $binary_remote_addr zone=addr:10m;
limit_conn addr 10;
```

#### CORS

```scala
// В Server.scala добавить CORS middleware
import org.http4s.server.middleware.CORS

val corsConfig = CORS.Policy
  .default
  .withAllowOriginHost(Set(
    Origin.Host(Uri.Scheme.https, Uri.RegName("yourdomain.com"), None)
  ))
  .withAllowCredentials(false)
  .withAllowedMethods(Some(Set(Method.GET, Method.POST, Method.PUT, Method.DELETE)))

val corsService = CORS(httpApp, corsConfig)
```

### 6. **Защита от CSRF**

#### Double Submit Cookie Pattern

```scala
// Генерация CSRF токена
val csrfToken = UUID.randomUUID().toString

// Установка в cookie (httpOnly=false для JavaScript доступа)
Set-Cookie: XSRF-TOKEN=<token>; Secure; SameSite=Strict

// Проверка в заголовке
X-XSRF-TOKEN: <token>
```

### 7. **Compliance & Privacy**

#### GDPR Compliance

- **Право на удаление**: Cascade delete для всех связанных данных
- **Шифрование PII**: SSN, payment info, медицинские данные
- **Audit trail**: Логирование доступа к чувствительным данным
- **Data minimization**: Собираем только необходимые данные

#### Data Retention

```sql
-- Автоочистка старых логов (90 дней)
DELETE FROM security_audit_log 
WHERE timestamp < NOW() - INTERVAL '90 days';

-- Автоочистка expired токенов
DELETE FROM refresh_tokens 
WHERE expires_at < NOW();

-- Автоочистка expired сессий
DELETE FROM user_sessions 
WHERE expires_at < NOW();
```

## 🔑 API Endpoints

### Public (без аутентификации)

```
POST /auth/register     - Регистрация
POST /auth/login        - Вход
POST /auth/refresh      - Обновление токена
GET  /health           - Health check
```

### Protected (требуется JWT)

```
POST /auth/logout       - Выход
GET  /api/users         - Список пользователей (Admin)
POST /api/users         - Создание пользователя (Admin)
GET  /api/specialists   - Список специалистов (All)
POST /api/consultations - Создание консультации (Client+)
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
    "phone": "+1234567890",
    "role": "client"
  }'

# Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": "2026-01-13T20:15:00Z",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
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

### 3. Использование JWT для защищенных endpoint'ов

```bash
curl -X GET http://localhost/api/users \
  -H "Authorization: Bearer eyJhbGc..."
```

### 4. Обновление токена

```bash
curl -X POST http://localhost/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### 5. Выход

```bash
curl -X POST http://localhost/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

## 🛡️ Security Best Practices

### Development

1. ✅ Никогда не коммитить секреты в git
2. ✅ Использовать .env файлы (добавлены в .gitignore)
3. ✅ Генерировать сильный JWT secret: `openssl rand -base64 64`
4. ✅ Разные секреты для dev/staging/prod

### Production

1. ✅ **HTTPS обязателен** - TLS 1.3
2. ✅ Хранить секреты в AWS Secrets Manager / Azure Key Vault
3. ✅ Регулярно ротировать JWT secret
4. ✅ Мониторинг подозрительной активности через audit logs
5. ✅ Backups с шифрованием (AWS RDS encrypted)
6. ✅ Network isolation (VPC, Security Groups)
7. ✅ WAF (Web Application Firewall) - AWS WAF / Cloudflare

### Secrets Management

**Рекомендуется использовать [Infisical](https://infisical.com) для управления секретами.**

Полное руководство: [INFISICAL_SETUP.md](INFISICAL_SETUP.md)

**Преимущества Infisical:**

- ✅ Централизованное хранилище (end-to-end encryption)
- ✅ Автоматическая rotation секретов
- ✅ Audit logs всех доступов
- ✅ RBAC для команды
- ✅ Интеграция с Kubernetes, Docker, CI/CD
- ✅ Version control и rollback секретов
- ✅ Dynamic secrets для PostgreSQL

```bash
# Setup с Infisical
infisical secrets set JWT_SECRET "$(openssl rand -base64 64)" --env=production
infisical secrets set DB_ENCRYPTION_KEY "$(openssl rand -base64 32)" --env=production
infisical secrets set SESSION_SECRET "$(openssl rand -base64 32)" --env=production

# Run приложения
infisical run --env=production -- sbt run
```

### Legacy: Environment Variables (.env files)

⚠️ Устаревший подход, используйте только для быстрого прототипирования.

```bash
# .env для production
JWT_SECRET=<strong-secret-key-64-chars>
JWT_ISSUER=consultant-api
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d
DB_ENCRYPTION_KEY=<db-encryption-key>
SESSION_SECRET=<session-secret>
```

## 📊 Security Monitoring

### CloudWatch Alarms (AWS)

- Failed login attempts > 100/min
- Account lockouts > 10/min
- Invalid JWT tokens > 50/min
- Unusual API access patterns

### Metrics to track

- Authentication success/failure rate
- Average login time
- Token validation failures
- Concurrent sessions per user
- Geographic distribution of logins

## 🔧 Интеграция с существующим кодом

### В build.sbt добавить зависимости

```scala
libraryDependencies ++= Seq(
  "com.github.jwt-scala" %% "jwt-circe" % "9.4.5",
  "org.bouncycastle" % "bcprov-jdk18on" % "1.77"
)
```

### В Server.scala

```scala
// Создаем security сервисы
val passwordService = new PasswordHashingService()
val jwtService = new JwtTokenService(
  secretKey = config.jwtSecret,
  issuer = config.jwtIssuer
)
val authService = new AuthenticationService(
  userRepo, credentialsRepo, refreshTokenRepo, 
  auditRepo, passwordService, jwtService
)

// Добавляем auth routes
val authRoutes = new AuthRoutes(authService)
val allRoutes = authRoutes.routes ++ userRoutes.routes ++ ...

// Добавляем authentication middleware
val authMiddleware = new AuthenticationMiddleware(jwtService)
```

Полная документация по безопасности готова! 🔒
