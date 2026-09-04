CREATE TABLE forum_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50) NOT NULL DEFAULT 'MessageSquare',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE forum_threads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES forum_categories(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    author_name VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'normal',
    view_count INT NOT NULL DEFAULT 0,
    last_activity TIMESTAMP NOT NULL DEFAULT now(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE forum_replies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id UUID NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    author_name VARCHAR(200),
    content TEXT NOT NULL,
    parent_reply_id UUID REFERENCES forum_replies(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_threads_category ON forum_threads (category_id);
CREATE INDEX idx_threads_status ON forum_threads (status);
CREATE INDEX idx_replies_thread ON forum_replies (thread_id);

INSERT INTO forum_categories (id, name, description, icon) VALUES
    ('11111111-1111-1111-1111-111111111111', 'General Discussion', 'General conversations about club activities and community news.', 'MessageSquare'),
    ('22222222-2222-2222-2222-222222222222', 'Events & Projects', 'Discuss upcoming and past events, share ideas for new projects.', 'Calendar'),
    ('33333333-3333-3333-3333-333333333333', 'Members Corner', 'A space for members to connect, share updates, and collaborate.', 'Users'),
    ('44444444-4444-4444-4444-444444444444', 'Suggestions & Feedback', 'Share your ideas to improve the club and its initiatives.', 'Lightbulb');
