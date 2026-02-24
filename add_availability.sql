-- Add availability for all specialists without it
INSERT INTO specialist_availability (id, specialist_id, day_of_week, start_time, end_time, created_at, updated_at)
SELECT
  gen_random_uuid(),
  s.id,
  d.day,
  '09:00:00',
  '17:00:00',
  NOW(),
  NOW()
FROM specialists s
CROSS JOIN (VALUES (0), (1), (2), (3), (4), (5), (6)) AS d(day)
WHERE s.id NOT IN (SELECT DISTINCT specialist_id FROM specialist_availability);

-- Verify
SELECT s.name, s.email, COUNT(a.id) as slots
FROM specialists s
LEFT JOIN specialist_availability a ON s.id = a.specialist_id
GROUP BY s.id, s.name, s.email
ORDER BY s.name;
