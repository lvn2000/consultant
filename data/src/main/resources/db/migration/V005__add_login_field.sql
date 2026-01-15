-- Add login field to users table
ALTER TABLE users ADD COLUMN login VARCHAR(255) UNIQUE;

-- Update existing users to have login based on email (temporary)
UPDATE users SET login = email WHERE login IS NULL;

-- Make login NOT NULL after populating
ALTER TABLE users ALTER COLUMN login SET NOT NULL;

-- Create index on login
CREATE INDEX idx_users_login ON users(login);