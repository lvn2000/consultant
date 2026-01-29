-- Remove UNIQUE constraint from countries.code to allow duplicate phone codes
ALTER TABLE countries DROP CONSTRAINT countries_code_key;