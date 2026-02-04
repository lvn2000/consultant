-- Create specialist availability table
CREATE TABLE specialist_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6), -- 0=Monday, 6=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_time CHECK (start_time < end_time),
    CONSTRAINT unique_specialist_slot UNIQUE(specialist_id, day_of_week, start_time, end_time)
);

-- Create index for efficient queries
CREATE INDEX idx_specialist_availability_specialist_id ON specialist_availability(specialist_id);
CREATE INDEX idx_specialist_availability_day_of_week ON specialist_availability(day_of_week);
