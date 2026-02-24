-- Add category rates for all specialists
INSERT INTO specialist_categories (specialist_id, category_id)
SELECT
  s.id,
  c.id
FROM specialists s
CROSS JOIN categories c
WHERE NOT EXISTS (
  SELECT 1 FROM specialist_categories sc
  WHERE sc.specialist_id = s.id AND sc.category_id = c.id
);

-- Verify
SELECT s.name, c.name as category
FROM specialists s
JOIN specialist_categories sc ON s.id = sc.specialist_id
JOIN categories c ON sc.category_id = c.id
ORDER BY s.name, c.name;
