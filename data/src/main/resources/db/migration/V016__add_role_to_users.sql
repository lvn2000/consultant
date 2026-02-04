-- Add role column to users table
ALTER TABLE users ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'Client';

-- Add password_hash column to users table (temporary - will be moved to credentials)
ALTER TABLE users ADD COLUMN password_hash VARCHAR(512);

-- Create index on role
CREATE INDEX idx_users_role ON users(role);

-- Update existing users to have appropriate roles based on whether they have specialist profiles
UPDATE users SET role = 'Specialist' WHERE id IN (SELECT id FROM specialists);
UPDATE users SET role = 'Client' WHERE role = 'Client';