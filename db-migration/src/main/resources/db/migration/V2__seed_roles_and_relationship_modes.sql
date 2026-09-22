INSERT INTO role (code, name, description, is_system) VALUES
    ('USER', 'User', 'Standard member', TRUE),
    ('MODERATOR', 'Moderator', 'Content moderator', TRUE),
    ('ADMIN', 'Admin', 'Platform administrator', TRUE),
    ('SUPER_ADMIN', 'Super Admin', 'Full platform access', TRUE);

INSERT INTO relationship_mode (code, name, description, display_order, is_active) VALUES
    ('DATING', 'Dating / Love', 'Dating and romantic connection', 1, TRUE),
    ('FRIENDSHIP', 'Friendship', 'Friendship and companionship', 2, TRUE),
    ('MARRIAGE', 'Marriage', 'Marriage-oriented matching', 3, TRUE);
