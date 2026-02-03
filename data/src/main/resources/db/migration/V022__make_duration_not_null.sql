-- Keep duration nullable initially
-- It will be set by specialist when they approve the consultation
-- This migration is now reverted since duration should remain optional

-- NOTE: Duration is optional when client creates request
-- Specialist sets duration when approving consultation
-- This allows specialist to estimate time based on problem description
