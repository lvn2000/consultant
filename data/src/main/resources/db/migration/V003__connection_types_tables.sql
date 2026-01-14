-- Connection types table
CREATE TABLE IF NOT EXISTS connection_types (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_connection_types_name ON connection_types(name);

-- Specialist connections junction table (one specialist can have many connection types)
CREATE TABLE IF NOT EXISTS specialist_connections (
    id UUID PRIMARY KEY,
    specialist_id UUID REFERENCES specialists(id) ON DELETE CASCADE NOT NULL,
    connection_type_id UUID REFERENCES connection_types(id) ON DELETE CASCADE NOT NULL,
    connection_value VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (specialist_id, connection_type_id)
);

CREATE INDEX idx_specialist_connections_specialist ON specialist_connections(specialist_id);
CREATE INDEX idx_specialist_connections_type ON specialist_connections(connection_type_id);
CREATE INDEX idx_specialist_connections_verified ON specialist_connections(is_verified);

-- Insert default connection types
INSERT INTO connection_types (id, name, description, created_at, updated_at) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Viber', 'Viber messaging', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 'WhatsApp', 'WhatsApp messaging', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440003', 'Slack', 'Slack messaging', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440004', 'Telegram', 'Telegram messaging', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440005', 'Skype', 'Skype communication', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440006', 'Discord', 'Discord messaging', NOW(), NOW())
ON CONFLICT DO NOTHING;
