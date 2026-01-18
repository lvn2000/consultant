-- Client connections table (parallel to specialist_connections)
CREATE TABLE IF NOT EXISTS client_connections (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    connection_type_id UUID REFERENCES connection_types(id) ON DELETE CASCADE NOT NULL,
    connection_value VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, connection_type_id)
);

CREATE INDEX idx_client_connections_user ON client_connections(user_id);
CREATE INDEX idx_client_connections_type ON client_connections(connection_type_id);
CREATE INDEX idx_client_connections_verified ON client_connections(is_verified);
