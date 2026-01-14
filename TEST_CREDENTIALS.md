# Test Credentials

This document contains the default test credentials loaded during database migration (V004).

## Test Accounts

### 1. Test User (Client)
- **Email:** `user@example.com`
- **Password:** `user`
- **Role:** Client
- **Name:** Test User
- **Phone:** +1234567890
- **User ID:** `11111111-1111-1111-1111-111111111111`

### 2. Test Specialist
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

These credentials are created by the V004 migration script:
```
V004__initial_test_data.sql
```

The migration:
1. Creates the test user and specialist user accounts
2. Inserts bcrypt-hashed passwords into the credentials table
3. Creates the specialist profile record
4. Adds a connection type (WhatsApp) for the specialist
5. Links the specialist to the first available category

## Password Hashes

The passwords are stored using bcrypt hashing:
- `user` → `$2a$10$N9qo8uLOickgx2ZMRZoMye.mw5qMRH.6V6tz8tNl0VTXTlCLBW3Se`
- `spec` → `$2a$10$QUghbfJsJpd6H/9xjZyO9eG7wZWEGN7fN4Vl5OZ8Z.1W4QxKzL9ey`

## Removing Test Data

To remove the test data in development:
```sql
DELETE FROM specialist_connections WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialist_categories WHERE specialist_id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM specialists WHERE id = '22222222-2222-2222-2222-222222222222'::uuid;
DELETE FROM credentials WHERE email IN ('user@example.com', 'spec@example.com');
DELETE FROM users WHERE id IN ('11111111-1111-1111-1111-111111111111'::uuid, '22222222-2222-2222-2222-222222222222'::uuid);
```

## Security Note

⚠️ **IMPORTANT:** These are test credentials for development and testing purposes only. 
- Do NOT use these credentials in production
- Always use strong, unique passwords in production
- Implement proper password hashing (bcrypt/Argon2) with salts
- Rotate credentials regularly
