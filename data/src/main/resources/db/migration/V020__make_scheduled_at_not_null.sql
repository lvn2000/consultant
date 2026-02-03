-- Make scheduled_at NOT NULL in consultations table
-- This ensures that all consultations must have a scheduled date/time

ALTER TABLE consultations
ALTER COLUMN scheduled_at SET NOT NULL;
