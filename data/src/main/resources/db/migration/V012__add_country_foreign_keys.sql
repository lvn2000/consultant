-- Add country_id columns to users and specialists tables with foreign key constraints

-- Add country_id to users table
ALTER TABLE users ADD COLUMN country_id UUID REFERENCES countries(id) ON DELETE SET NULL;

-- Add country_id to specialists table
ALTER TABLE specialists ADD COLUMN country_id UUID REFERENCES countries(id) ON DELETE SET NULL;

-- Create indexes for the new foreign key columns
CREATE INDEX idx_users_country_id ON users(country_id);
CREATE INDEX idx_specialists_country_id ON specialists(country_id);