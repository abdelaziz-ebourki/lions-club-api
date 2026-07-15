ALTER TABLE events ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'COMMUNITY';
CREATE INDEX idx_events_category ON events (category);
