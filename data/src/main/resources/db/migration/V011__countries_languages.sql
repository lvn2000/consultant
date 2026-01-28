
-- Create countries table
CREATE TABLE IF NOT EXISTS countries (
	id UUID PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	code VARCHAR(10) NOT NULL UNIQUE
);

-- Create languages table
CREATE TABLE IF NOT EXISTS languages (
	id UUID PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	code VARCHAR(10) NOT NULL UNIQUE
);

-- Create user_languages join table
CREATE TABLE IF NOT EXISTS user_languages (
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	language_id UUID NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
	PRIMARY KEY (user_id, language_id)
);

-- Create specialist_languages join table
CREATE TABLE IF NOT EXISTS specialist_languages (
	specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
	language_id UUID NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
	PRIMARY KEY (specialist_id, language_id)
);
