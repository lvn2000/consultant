-- Specialist category rates

CREATE TABLE IF NOT EXISTS specialist_category_rates (
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    hourly_rate NUMERIC(10, 2) NOT NULL,
    PRIMARY KEY (specialist_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_spec_category_rates_category_id
    ON specialist_category_rates(category_id);

-- Seed from existing specialist_categories using specialist base hourly rate
INSERT INTO specialist_category_rates (specialist_id, category_id, hourly_rate)
SELECT sc.specialist_id, sc.category_id, 0
FROM specialist_categories sc
ON CONFLICT (specialist_id, category_id) DO NOTHING;
