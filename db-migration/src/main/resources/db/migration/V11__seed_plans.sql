-- Placeholder pricing and feature lists so the subscription flow is fully testable end-to-end. Actual pricing,
-- billing periods on offer and the real feature list are a business/product decision, not a technical one -
-- same reasoning as HelpContentService's placeholder legal text and discovery_collection's seed row. Revisit
-- before going live.
INSERT INTO plan (code, name, description, price, currency_code, billing_period, duration_days, features, is_active, display_order)
VALUES
    ('PREMIUM_MONTHLY', 'Premium Monthly', 'Full access to premium features, billed monthly.',
     999.00, 'INR', 'MONTHLY', 30,
     '{"unlimitedInterests": true, "seeWhoViewedYou": true, "prioritySupport": false}'::jsonb,
     TRUE, 0),
    ('PREMIUM_QUARTERLY', 'Premium Quarterly', 'Full access to premium features, billed every 3 months.',
     2499.00, 'INR', 'QUARTERLY', 90,
     '{"unlimitedInterests": true, "seeWhoViewedYou": true, "prioritySupport": true}'::jsonb,
     TRUE, 1),
    ('PREMIUM_YEARLY', 'Premium Yearly', 'Full access to premium features, billed annually - best value.',
     7999.00, 'INR', 'YEARLY', 365,
     '{"unlimitedInterests": true, "seeWhoViewedYou": true, "prioritySupport": true}'::jsonb,
     TRUE, 2);
