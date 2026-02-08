-- Add functional indexes for case-insensitive login and email lookups
-- This allows efficient LOWER(column) = LOWER(value) queries in the login method
-- The queries use LOWER() to support case-insensitive login, which would normally
-- prevent index usage on regular btree indexes. These functional indexes solve this.

-- Create a functional index on LOWER(login) for case-insensitive login queries
CREATE INDEX idx_users_login_lower ON users(LOWER(login));

-- Create a functional index on LOWER(email) for case-insensitive email queries
CREATE INDEX idx_users_email_lower ON users(LOWER(email));
