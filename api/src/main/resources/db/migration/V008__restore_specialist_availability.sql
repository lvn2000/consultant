-- Restore specialist availability on specialists table

ALTER TABLE specialists
  ADD COLUMN IF NOT EXISTS is_available BOOLEAN NOT NULL DEFAULT TRUE;
