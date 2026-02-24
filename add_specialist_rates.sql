-- Add category rates for all specialists
INSERT INTO specialist_category_rates (specialist_id, category_id, hourly_rate, experience_years, rating, total_consultations)
SELECT
  s.id,
  c.id,
  50.00,
  5,
  NULL,
  0
FROM specialists s
CROSS JOIN categories c
WHERE NOT EXISTS (
  SELECT 1 FROM specialist_category_rates scr
  WHERE scr.specialist_id = s.id AND scr.category_id = c.id
);

-- Verify
SELECT s.name as specialist, c.name as category, scr.hourly_rate
FROM specialists s
JOIN specialist_category_rates scr ON s.id = scr.specialist_id
JOIN categories c ON scr.category_id = c.id
ORDER BY s.name, c.name
LIMIT 10;
