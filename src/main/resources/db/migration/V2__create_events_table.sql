CREATE TABLE events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    start_date_time TIMESTAMP   NOT NULL,
    end_date_time   TIMESTAMP   NOT NULL,
    location        VARCHAR(255),
    address         TEXT,
    max_attendees   INTEGER,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_start_date ON events (start_date_time);
CREATE INDEX idx_events_created_by ON events (created_by);
