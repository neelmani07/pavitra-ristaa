-- One placeholder collection so the read endpoints have real content rather than always returning an empty
-- list. Which collections to offer and how to curate them is a product decision, not a technical one - same
-- reasoning as HelpContentService's placeholder legal text. Empty criteria ({}) means "the full discoverable,
-- not-blocked pool, most recently active first" - the same default ordering browse()/home() already use.
-- More collections (or narrower criteria, e.g. {"minAge": 25, "maxAge": 35}) can be added with a future
-- migration or a future admin endpoint - no code change needed, since membership is computed from criteria
-- at read time.
INSERT INTO discovery_collection (code, name, description, criteria, display_order, is_active, created_at, updated_at)
VALUES (
    'new-members',
    'New to Pavitra Ristaa',
    'Recently active profiles across every relationship mode.',
    '{}'::jsonb,
    0,
    TRUE,
    now(),
    now()
);
