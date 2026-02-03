-- Insert initial test categories
INSERT INTO categories (id, name, description, parent_id) VALUES
('550e8400-e29b-41d4-a716-446655440000'::uuid, 'Technology', 'Technology and software development services', NULL),
('550e8400-e29b-41d4-a716-446655440001'::uuid, 'Business', 'Business consulting and strategy services', NULL),
('550e8400-e29b-41d4-a716-446655440002'::uuid, 'Design', 'Design and creative services', NULL),
('550e8400-e29b-41d4-a716-446655440003'::uuid, 'Marketing', 'Marketing and advertising services', NULL),
('550e8400-e29b-41d4-a716-446655440004'::uuid, 'Legal', 'Legal consulting and services', NULL),
('550e8400-e29b-41d4-a716-446655440005'::uuid, 'Finance', 'Financial consulting and services', NULL),
('550e8400-e29b-41d4-a716-446655440006'::uuid, 'Healthcare', 'Healthcare consulting and services', NULL),
('550e8400-e29b-41d4-a716-446655440007'::uuid, 'Education', 'Education and training services', NULL)
ON CONFLICT (id) DO NOTHING;