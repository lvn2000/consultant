# Test Credentials

This document contains the default test credentials loaded during database migrations.

## Prerequisites

Before using these credentials, ensure the backend is configured with the correct database connection:

```bash
# Database credentials in .env file (from .env.example):
DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://localhost:5432/consultant
DB_USER=postgres
DB_PASSWORD=postgres
DB_POOL_SIZE=32
```

## Test Accounts

### 1. Test Admin
- **Email:** `admin@admin.com`
- **Password:** `admin`
- **Role:** Admin
- **Name:** Administrator
- **User ID:** `99999999-9999-9999-9999-999999999999`

### 2. Test User (Client)
- **Email:** `user@example.com`
- **Password:** `user`
- **Role:** Client
- **Name:** Test User
- **Phone:** +1234567890
- **User ID:** `11111111-1111-1111-1111-111111111111`

### 3. Test Specialist
- **Email:** `spec@example.com`
- **Password:** `spec`
- **Role:** Specialist
- **Name:** Test Specialist
- **Phone:** +9876543210
- **User ID:** `22222222-2222-2222-2222-222222222222`
- **Hourly Rate:** $50.00
- **Experience:** 5 years
- **Available:** Yes
- **WhatsApp:** +9876543210

## Database Migration

These credentials are created by the database migration script:

```
V002__seed_data.sql
```

The migration:
1. Creates the admin, test user, and specialist user accounts
2. Inserts bcrypt-hashed passwords into the credentials table
3. Creates the specialist profile record
4. Adds connection types and specialist connections
5. Creates categories and links the specialist to categories

## Password Hashes

The passwords are stored using bcrypt hashing:
- `admin` → `$2a$10$OQnLnzqlG4ulnNKTz3MQZOz8YzuugeWGaGTMJhQkcSxtYlaUrMKrq`
- `user` → `$2a$10$l1GeFWm4m1cZTuVIoEuRQObiF7hX1muH6/CSELnpmSoQ/sRSIxadi`
- `spec` → `$2a$06$1dB6e0xAh4TG/CQk3CjcH.o5ktrQTQLUng6sDGyMveq5148SodjLe`

## Removing Test Data

To remove the test data in development:
```sql
DELETE FROM specialist_connections WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialist_categories WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialists WHERE id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM credentials WHERE email IN ('admin@admin.com', 'user@example.com', 'spec@example.com');
DELETE FROM users WHERE id IN ('99999999-9999-9999-9999-999999999999'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '22222222-2222-2222-2222-222222222222'::uuid);
```

## Security Note

⚠️ **IMPORTANT:** These are test credentials for development and testing purposes only. 
- Do NOT use these credentials in production
- Always use strong, unique passwords in production
- Implement proper password hashing (bcrypt/Argon2) with salts
- Rotate credentials regularly
