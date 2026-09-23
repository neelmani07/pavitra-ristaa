INSERT INTO report_reason (code, name, description, is_active) VALUES
    ('FAKE_PROFILE', 'Fake profile', 'This profile appears to be fake or impersonating someone', TRUE),
    ('INAPPROPRIATE_CONTENT', 'Inappropriate content', 'Photos, bio or messages contain inappropriate content', TRUE),
    ('HARASSMENT', 'Harassment or abuse', 'Threatening, abusive or harassing behavior', TRUE),
    ('SPAM', 'Spam', 'Unsolicited advertising or repetitive unwanted messages', TRUE),
    ('SCAM_OR_FRAUD', 'Scam or fraud', 'Asking for money or attempting to defraud', TRUE),
    ('UNDERAGE', 'Underage user', 'The user appears to be under the minimum age', TRUE),
    ('OFFENSIVE_LANGUAGE', 'Offensive language', 'Hate speech or offensive language', TRUE),
    ('IMPERSONATION', 'Impersonation', 'Pretending to be someone else', TRUE),
    ('OTHER', 'Other', 'Another reason not listed here', TRUE);
