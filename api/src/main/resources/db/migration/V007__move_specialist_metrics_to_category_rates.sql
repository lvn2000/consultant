-- Move specialist metrics to specialist_category_rates

ALTER TABLE specialist_category_rates
  ADD COLUMN IF NOT EXISTS experience_years INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS rating NUMERIC(10, 2),
  ADD COLUMN IF NOT EXISTS total_consultations INTEGER NOT NULL DEFAULT 0;

-- Remove specialist-level metrics
ALTER TABLE specialists
  DROP COLUMN IF EXISTS hourly_rate,
  DROP COLUMN IF EXISTS experience_years,
  DROP COLUMN IF EXISTS rating,
  DROP COLUMN IF EXISTS total_consultations;
