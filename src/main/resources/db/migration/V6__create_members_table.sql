CREATE TABLE members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT 'Member',
    bio TEXT,
    avatar_url VARCHAR(512),
    email VARCHAR(255),
    phone VARCHAR(50),
    social_linkedin VARCHAR(512),
    social_facebook VARCHAR(512),
    social_instagram VARCHAR(512),
    joined_at DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_members_role ON members (role);
CREATE INDEX idx_members_joined_at ON members (joined_at);
