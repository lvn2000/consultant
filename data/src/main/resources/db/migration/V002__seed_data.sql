-- =============================================================================
-- V002: Seed Data
-- Reference data (connection types, categories, countries) and test accounts.
-- All inserts use ON CONFLICT DO NOTHING for idempotency.
-- =============================================================================

-- =============================================================================
-- REFERENCE DATA: Connection Types
-- =============================================================================
INSERT INTO connection_types (id, name, description, created_at, updated_at) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Viber',    'Viber messaging',     NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 'WhatsApp', 'WhatsApp messaging',  NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440003', 'Slack',    'Slack messaging',     NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440004', 'Telegram', 'Telegram messaging',  NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440005', 'Skype',    'Skype communication', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440006', 'Discord',  'Discord messaging',   NOW(), NOW())
ON CONFLICT DO NOTHING;

-- =============================================================================
-- REFERENCE DATA: Categories
-- =============================================================================
INSERT INTO categories (id, name, description, parent_id) VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'Technology',  'Technology and software development services', NULL),
    ('550e8400-e29b-41d4-a716-446655440001', 'Business',    'Business consulting and strategy services',    NULL),
    ('550e8400-e29b-41d4-a716-446655440002', 'Design',      'Design and creative services',                 NULL),
    ('550e8400-e29b-41d4-a716-446655440003', 'Marketing',   'Marketing and advertising services',           NULL),
    ('550e8400-e29b-41d4-a716-446655440004', 'Legal',       'Legal consulting and services',                NULL),
    ('550e8400-e29b-41d4-a716-446655440005', 'Finance',     'Financial consulting and services',            NULL),
    ('550e8400-e29b-41d4-a716-446655440006', 'Healthcare',  'Healthcare consulting and services',           NULL),
    ('550e8400-e29b-41d4-a716-446655440007', 'Education',   'Education and training services',              NULL)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- REFERENCE DATA: Countries
-- =============================================================================
INSERT INTO countries (id, name, code, shortname) VALUES
    (gen_random_uuid(), 'Afghanistan',    '+093', 'AF'),
    (gen_random_uuid(), 'Albania',        '+355', 'AL'),
    (gen_random_uuid(), 'Algeria',        '+213', 'DZ'),
    (gen_random_uuid(), 'Argentina',      '+054', 'AR'),
    (gen_random_uuid(), 'Australia',      '+061', 'AU'),
    (gen_random_uuid(), 'Austria',        '+043', 'AT'),
    (gen_random_uuid(), 'Bangladesh',     '+880', 'BD'),
    (gen_random_uuid(), 'Belgium',        '+032', 'BE'),
    (gen_random_uuid(), 'Brazil',         '+055', 'BR'),
    (gen_random_uuid(), 'Canada',         '+001', 'CA'),
    (gen_random_uuid(), 'China',          '+086', 'CN'),
    (gen_random_uuid(), 'Denmark',        '+045', 'DK'),
    (gen_random_uuid(), 'Egypt',          '+020', 'EG'),
    (gen_random_uuid(), 'Finland',        '+358', 'FI'),
    (gen_random_uuid(), 'France',         '+033', 'FR'),
    (gen_random_uuid(), 'Germany',        '+049', 'DE'),
    (gen_random_uuid(), 'India',          '+091', 'IN'),
    (gen_random_uuid(), 'Italy',          '+039', 'IT'),
    (gen_random_uuid(), 'Japan',          '+081', 'JP'),
    (gen_random_uuid(), 'Mexico',         '+052', 'MX'),
    (gen_random_uuid(), 'Netherlands',    '+031', 'NL'),
    (gen_random_uuid(), 'Norway',         '+047', 'NO'),
    (gen_random_uuid(), 'Poland',         '+048', 'PL'),
    (gen_random_uuid(), 'Russia',         '+007', 'RU'),
    (gen_random_uuid(), 'South Korea',    '+082', 'KR'),
    (gen_random_uuid(), 'Spain',          '+034', 'ES'),
    (gen_random_uuid(), 'Sweden',         '+046', 'SE'),
    (gen_random_uuid(), 'Switzerland',    '+041', 'CH'),
    (gen_random_uuid(), 'Turkey',         '+090', 'TR'),
    (gen_random_uuid(), 'Ukraine',        '+380', 'UA'),
    (gen_random_uuid(), 'United Kingdom', '+044', 'GB'),
    (gen_random_uuid(), 'United States',  '+001', 'US');

-- =============================================================================
-- TEST DATA: Admin account
-- WARNING: Default admin (login: admin, password: admin) is for initial setup!
--          Change the password or remove this user in production!
-- =============================================================================
INSERT INTO users (id, login, email, name, phone, role, created_at, updated_at)
VALUES (
    '99999999-9999-9999-9999-999999999999'::uuid,
    'admin', 'admin@admin.com', 'Administrator', NULL, 'Admin',
    NOW(), NOW()
) ON CONFLICT DO NOTHING;

INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'admin@admin.com',
    '$2a$10$kShylGbn/AAM0WFbEPzOLeO36bv8MElKe2jx.s.k6L41wC0uyGqhK',
    'random_salt_admin',
    '99999999-9999-9999-9999-999999999999'::uuid,
    'Admin', true, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;

-- =============================================================================
-- TEST DATA: Client account (login: user, password: user)
-- =============================================================================
INSERT INTO users (id, login, email, name, phone, role, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'user', 'user@example.com', 'Test User', '+1234567890', 'Client',
    NOW(), NOW()
) ON CONFLICT DO NOTHING;

INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'user@example.com',
    '$2a$10$JHe28ZfbIe7n7cxDgPakW.YxEvJt.WOzNo4rtxgDPR24of3olyCbm',
    'random_salt_user',
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Client', true, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;

-- =============================================================================
-- TEST DATA: Specialist account (login: spec, password: spec)
-- =============================================================================
INSERT INTO users (id, login, email, name, phone, role, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'spec', 'spec@example.com', 'Test Specialist', '+9876543210', 'Specialist',
    NOW(), NOW()
) ON CONFLICT DO NOTHING;

INSERT INTO specialists (
    id, email, name, phone, bio, hourly_rate, experience_years,
    rating, total_consultations, is_available, created_at, updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'spec@example.com', 'Test Specialist', '+9876543210',
    'Professional test specialist with extensive experience',
    50.00, 5, NULL, 0, true, NOW(), NOW()
) ON CONFLICT DO NOTHING;

INSERT INTO credentials (email, password_hash, salt, user_id, role, is_active, created_at, updated_at)
VALUES (
    'spec@example.com',
    '$2a$10$Klbk5q7yO3JNWkDajm4lMOhfJp.NRFRqDr8WmCOJxZ2bmysRllSYa',
    'random_salt_spec',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'Specialist', true, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;

-- Link test specialist to first category
INSERT INTO specialist_categories (specialist_id, category_id)
SELECT '22222222-2222-2222-2222-222222222222'::uuid, id
FROM categories LIMIT 1
ON CONFLICT DO NOTHING;

-- Test specialist connection (WhatsApp)
INSERT INTO specialist_connections (
    id, specialist_id, connection_type_id, connection_value, is_verified, created_at, updated_at
) VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    '+9876543210', true, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- Test specialist category rates
INSERT INTO specialist_category_rates (specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations)
SELECT
    '22222222-2222-2222-2222-222222222222'::uuid,
    c.id,
    CASE c.name
        WHEN 'Technology' THEN 75.00
        WHEN 'Business'   THEN 80.00
        WHEN 'Finance'    THEN 85.00
        WHEN 'Healthcare' THEN 70.00
        WHEN 'Education'  THEN 65.00
        WHEN 'Legal'      THEN 90.00
        WHEN 'Marketing'  THEN 60.00
        ELSE 50.00
    END,
    CASE c.name
        WHEN 'Technology' THEN 8
        WHEN 'Business'   THEN 10
        WHEN 'Finance'    THEN 7
        WHEN 'Healthcare' THEN 6
        WHEN 'Education'  THEN 9
        WHEN 'Legal'      THEN 12
        WHEN 'Marketing'  THEN 5
        ELSE 3
    END,
    CASE c.name
        WHEN 'Technology' THEN 4.8
        WHEN 'Business'   THEN 4.9
        WHEN 'Finance'    THEN 4.7
        WHEN 'Healthcare' THEN 4.6
        WHEN 'Legal'      THEN 4.9
        WHEN 'Marketing'  THEN 4.5
        ELSE 4.0
    END,
    CASE c.name
        WHEN 'Technology' THEN 45
        WHEN 'Business'   THEN 52
        WHEN 'Finance'    THEN 38
        WHEN 'Healthcare' THEN 29
        WHEN 'Education'  THEN 67
        WHEN 'Legal'      THEN 41
        WHEN 'Marketing'  THEN 33
        ELSE 15
    END
FROM categories c
ON CONFLICT (specialist_id, category_id) DO NOTHING;
