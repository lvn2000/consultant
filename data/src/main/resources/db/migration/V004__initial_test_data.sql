--
-- WARNING: The following default admin user (login: admin, password: admin) is for initial setup only.
--          You MUST change the password or remove this user after installation for security reasons!
--

-- Insert default admin user (login: admin, password: admin)
INSERT INTO users (id, login, email, name, phone, created_at, updated_at)
VALUES (
    '99999999-9999-9999-9999-999999999999'::uuid,
    'admin',
    'admin@admin.com',
    'Administrator',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Insert credentials for default admin
-- Password hash for 'admin' (bcrypt: $2a$10$7EqJtq98hPqEX7fNZaFWoOhi5g1bY6LZix/2yW0b1p/0t6kQ6F7Ui)
INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'admin@admin.com',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5g1bY6LZix/2yW0b1p/0t6kQ6F7Ui',  -- bcrypt hash of 'admin'
    'random_salt_admin',
    '99999999-9999-9999-9999-999999999999'::uuid,
    'Admin',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
-- Initial test data migration
-- Creates test user and specialist accounts

-- Insert test user (email: user@example.com, password: user)
INSERT INTO users (id, email, name, phone, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'user@example.com',
    'Test User',
    '+1234567890',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Insert credentials for test user
-- Note: In production, use proper password hashing (BCrypt/Argon2)
-- Password hash for 'user' (using simple SHA256 for demo: should be bcrypt in production)
INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'user@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.mw5qMRH.6V6tz8tNl0VTXTlCLBW3Se',  -- bcrypt hash of 'user'
    'random_salt_user',
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Client',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Insert test specialist (email: spec@example.com, password: spec)
INSERT INTO users (id, email, name, phone, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'spec@example.com',
    'Test Specialist',
    '+9876543210',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Insert specialist profile
INSERT INTO specialists (
    id, email, name, phone, bio, hourly_rate, experience_years,
    rating, total_consultations, is_available, created_at, updated_at
)
VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'spec@example.com',
    'Test Specialist',
    '+9876543210',
    'Professional test specialist with extensive experience',
    50.00,
    5,
    NULL,
    0,
    true,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Insert credentials for test specialist
-- Password hash for 'spec' (using bcrypt)
INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'spec@example.com',
    '$2a$10$QUghbfJsJpd6H/9xjZyO9eG7wZWEGN7fN4Vl5OZ8Z.1W4QxKzL9ey',  -- bcrypt hash of 'spec'
    'random_salt_spec',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'Specialist',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Connect specialist to a category (if any exist, use first one)
INSERT INTO specialist_categories (specialist_id, category_id)
SELECT '22222222-2222-2222-2222-222222222222'::uuid, id
FROM categories
LIMIT 1
ON CONFLICT DO NOTHING;

-- Insert test connection type if not exists
INSERT INTO connection_types (id, name, description, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    'WhatsApp',
    'WhatsApp messaging',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Add test specialist connection
INSERT INTO specialist_connections (
    id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
)
VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    '+9876543210',
    true,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
