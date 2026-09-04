CREATE TABLE gallery_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_url VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512),
    category VARCHAR(30) NOT NULL,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    uploaded_at TIMESTAMP NOT NULL DEFAULT now(),
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_gallery_category ON gallery_items (category);
CREATE INDEX idx_gallery_event ON gallery_items (event_id);
CREATE INDEX idx_gallery_uploaded_at ON gallery_items (uploaded_at);
