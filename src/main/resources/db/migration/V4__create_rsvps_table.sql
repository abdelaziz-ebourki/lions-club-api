CREATE TABLE rsvps (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    member_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(8)  NOT NULL CHECK (status IN ('YES', 'NO', 'MAYBE')),
    plus_one    INTEGER     NOT NULL DEFAULT 0 CHECK (plus_one >= 0),
    notes       TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (event_id, member_id)
);

CREATE INDEX idx_rsvps_event_status ON rsvps (event_id, status);
CREATE INDEX idx_rsvps_member ON rsvps (member_id);
