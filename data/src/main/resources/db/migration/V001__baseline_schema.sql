-- =============================================================================
-- V001: Baseline Schema
-- Consolidated from all previous migrations into a single clean baseline.
-- Creates the complete database structure for the Consultant platform.
-- =============================================================================

-- Required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- CORE TABLES
-- =============================================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    login VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL DEFAULT 'Client',
    password_hash VARCHAR(512),
    country_id UUID,
    encrypted_ssn BYTEA,
    encrypted_payment_info BYTEA,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

COMMENT ON COLUMN users.encrypted_ssn IS 'Encrypted with pgcrypto using AES-256';
COMMENT ON COLUMN users.encrypted_payment_info IS 'Encrypted payment information';

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_login_lower ON users(LOWER(login));
CREATE INDEX idx_users_email_lower ON users(LOWER(email));
CREATE INDEX idx_users_country_id ON users(country_id);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_parent ON categories(parent_id);

-- Specialists table
CREATE TABLE IF NOT EXISTS specialists (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    bio TEXT NOT NULL,
    hourly_rate DECIMAL(10, 2) NOT NULL,
    experience_years INTEGER NOT NULL,
    rating DECIMAL(3, 2),
    total_consultations INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    country_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_specialists_email ON specialists(email);
CREATE INDEX idx_specialists_rating ON specialists(rating);
CREATE INDEX idx_specialists_rate ON specialists(hourly_rate);
CREATE INDEX idx_specialists_country_id ON specialists(country_id);

-- Specialist categories junction table (legacy, kept for compatibility)
CREATE TABLE IF NOT EXISTS specialist_categories (
    specialist_id UUID REFERENCES specialists(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (specialist_id, category_id)
);

CREATE INDEX idx_specialist_categories_specialist ON specialist_categories(specialist_id);
CREATE INDEX idx_specialist_categories_category ON specialist_categories(category_id);

-- Specialist category rates (extended junction with rates/experience)
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

-- Consultations table
CREATE TABLE IF NOT EXISTS consultations (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    specialist_id UUID REFERENCES specialists(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    duration INTEGER,
    price DECIMAL(10, 2) NOT NULL,
    is_free BOOLEAN DEFAULT false,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_consultations_user ON consultations(user_id);
CREATE INDEX idx_consultations_specialist ON consultations(specialist_id);
CREATE INDEX idx_consultations_status ON consultations(status);
CREATE INDEX idx_consultations_created ON consultations(created_at);
CREATE INDEX idx_consultations_is_free ON consultations(is_free);

-- =============================================================================
-- SECURITY TABLES
-- =============================================================================

-- Credentials table
CREATE TABLE IF NOT EXISTS credentials (
    email VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(512) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credentials_user_id ON credentials(user_id);
CREATE INDEX idx_credentials_email ON credentials(email);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    token VARCHAR(512) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Security audit log
CREATE TABLE IF NOT EXISTS security_audit_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX idx_audit_user_id ON security_audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON security_audit_log(timestamp);
CREATE INDEX idx_audit_action ON security_audit_log(action);

-- User sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(512) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON user_sessions(expires_at);

-- Auto-update trigger for credentials.updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_credentials_updated_at
    BEFORE UPDATE ON credentials
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- CONNECTION TYPES & CONNECTIONS
-- =============================================================================

-- Connection types table
CREATE TABLE IF NOT EXISTS connection_types (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_connection_types_name ON connection_types(name);

-- Specialist connections (specialist <-> connection_type)
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

-- Client connections (user <-> connection_type)
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

-- =============================================================================
-- GEOGRAPHY & LANGUAGES
-- =============================================================================

-- Countries table
CREATE TABLE IF NOT EXISTS countries (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    shortname VARCHAR(50)
);

-- Add foreign keys from users and specialists to countries
ALTER TABLE users ADD CONSTRAINT fk_users_country
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE SET NULL;

ALTER TABLE specialists ADD CONSTRAINT fk_specialists_country
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE SET NULL;

-- Languages table
CREATE TABLE IF NOT EXISTS languages (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE
);

-- User languages (user <-> language)
CREATE TABLE IF NOT EXISTS user_languages (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, language_id)
);

-- Specialist languages (specialist <-> language)
CREATE TABLE IF NOT EXISTS specialist_languages (
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    PRIMARY KEY (specialist_id, language_id)
);

-- =============================================================================
-- AVAILABILITY & NOTIFICATIONS
-- =============================================================================

-- Specialist availability slots
CREATE TABLE IF NOT EXISTS specialist_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_time CHECK (start_time < end_time),
    CONSTRAINT unique_specialist_slot UNIQUE(specialist_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_specialist_availability_specialist_id ON specialist_availability(specialist_id);
CREATE INDEX idx_specialist_availability_day_of_week ON specialist_availability(day_of_week);

-- Notification preferences
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, notification_type)
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);
CREATE INDEX idx_notification_preferences_type ON notification_preferences(notification_type);
