-- =============================================================================
-- V003: Add System Settings Table
-- Adds configurable system-wide settings including idle timeout
-- =============================================================================

-- System settings table for admin-configurable values
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(255) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    setting_type VARCHAR(50) NOT NULL DEFAULT 'integer', -- integer, string, boolean, json
    description TEXT,
    is_public BOOLEAN DEFAULT true, -- true = accessible via API, false = internal only
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_settings_key ON system_settings(setting_key);
CREATE INDEX idx_system_settings_public ON system_settings(is_public);

COMMENT ON TABLE system_settings IS 'System-wide configurable settings';
COMMENT ON COLUMN system_settings.setting_key IS 'Unique identifier for the setting (e.g., idle_timeout_minutes)';
COMMENT ON COLUMN system_settings.setting_value IS 'Value stored as text, parsed based on setting_type';
COMMENT ON COLUMN system_settings.is_public IS 'If true, can be read by authenticated users via API';

-- Insert default idle timeout setting (30 minutes)
INSERT INTO system_settings (id, setting_key, setting_value, setting_type, description, is_public)
VALUES (
    gen_random_uuid(),
    'idle_timeout_minutes',
    '30',
    'integer',
    'Number of minutes of user inactivity before automatic logout',
    true
) ON CONFLICT (setting_key) DO NOTHING;

-- Insert session warning time (5 minutes before timeout)
INSERT INTO system_settings (id, setting_key, setting_value, setting_type, description, is_public)
VALUES (
    gen_random_uuid(),
    'idle_warning_minutes',
    '5',
    'integer',
    'Number of minutes before timeout to show warning to user',
    true
) ON CONFLICT (setting_key) DO NOTHING;

-- Add index for session timeout tracking
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_activity ON user_sessions(last_activity);
