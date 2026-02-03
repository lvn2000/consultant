-- Add specialist_category_rates table to replace the old specialist_categories junction table
-- This table stores the rates and experience for each specialist-category combination

CREATE TABLE IF NOT EXISTS specialist_category_rates (
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    hourly_rate DECIMAL(10, 2) NOT NULL,
    experience_years INTEGER NOT NULL DEFAULT 0,
    rating DECIMAL(3, 2),
    total_consultations INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (specialist_id, category_id)
);

CREATE INDEX idx_specialist_category_rates_specialist ON specialist_category_rates(specialist_id);
CREATE INDEX idx_specialist_category_rates_category ON specialist_category_rates(category_id);
CREATE INDEX idx_specialist_category_rates_rate ON specialist_category_rates(hourly_rate);
CREATE INDEX idx_specialist_category_rates_rating ON specialist_category_rates(rating);

-- Migrate existing data from specialist_categories to specialist_category_rates
-- Use default values for rates and experience since the old table didn't have this data
INSERT INTO specialist_category_rates (specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations)
SELECT
    sc.specialist_id,
    sc.category_id,
    50.00,  -- Default hourly rate
    5,      -- Default experience years
    NULL,   -- Default rating
    0       -- Default total consultations
FROM specialist_categories sc
ON CONFLICT (specialist_id, category_id) DO NOTHING;

-- Add some test data for the existing test specialist
-- Insert category rates for the test specialist with different categories
INSERT INTO specialist_category_rates (specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations)
SELECT
    '22222222-2222-2222-2222-222222222222'::uuid,
    c.id,
    CASE
        WHEN c.name = 'Technology' THEN 75.00
        WHEN c.name = 'Business' THEN 80.00
        WHEN c.name = 'Finance' THEN 85.00
        WHEN c.name = 'Health' THEN 70.00
        WHEN c.name = 'Education' THEN 65.00
        WHEN c.name = 'Legal' THEN 90.00
        WHEN c.name = 'Marketing' THEN 60.00
        WHEN c.name = 'Consulting' THEN 85.00
        ELSE 50.00
    END,
    CASE
        WHEN c.name = 'Technology' THEN 8
        WHEN c.name = 'Business' THEN 10
        WHEN c.name = 'Finance' THEN 7
        WHEN c.name = 'Health' THEN 6
        WHEN c.name = 'Education' THEN 9
        WHEN c.name = 'Legal' THEN 12
        WHEN c.name = 'Marketing' THEN 5
        WHEN c.name = 'Consulting' THEN 11
        ELSE 3
    END,
    CASE
        WHEN c.name = 'Technology' THEN 4.8
        WHEN c.name = 'Business' THEN 4.9
        WHEN c.name = 'Finance' THEN 4.7
        WHEN c.name = 'Health' THEN 4.6
        WHEN c.name = 'Legal' THEN 4.9
        WHEN c.name = 'Marketing' THEN 4.5
        WHEN c.name = 'Consulting' THEN 4.8
        ELSE 4.0
    END,
    CASE
        WHEN c.name = 'Technology' THEN 45
        WHEN c.name = 'Business' THEN 52
        WHEN c.name = 'Finance' THEN 38
        WHEN c.name = 'Health' THEN 29
        WHEN c.name = 'Education' THEN 67
        WHEN c.name = 'Legal' THEN 41
        WHEN c.name = 'Marketing' THEN 33
        WHEN c.name = 'Consulting' THEN 58
        ELSE 15
    END
FROM categories c
WHERE c.id IN (
    SELECT id FROM categories
    WHERE name IN ('Technology', 'Business', 'Finance', 'Health', 'Education', 'Legal', 'Marketing', 'Consulting')
)
ON CONFLICT (specialist_id, category_id) DO NOTHING;