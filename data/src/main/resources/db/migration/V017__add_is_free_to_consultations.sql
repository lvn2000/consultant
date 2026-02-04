-- Add is_free column to consultations table
ALTER TABLE consultations ADD COLUMN is_free BOOLEAN DEFAULT false;

-- Create index on is_free for performance
CREATE INDEX idx_consultations_is_free ON consultations(is_free);