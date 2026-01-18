-- Correct the test specialist password so that login "spec" / "spec" works
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE credentials c
SET password_hash = crypt('spec', gen_salt('bf')),
    updated_at     = NOW()
FROM users u
WHERE c.user_id = u.id
  AND lower(u.login) = 'spec'
  AND c.is_active = true;
