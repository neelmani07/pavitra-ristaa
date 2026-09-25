-- One placeholder coupon so the validate/checkout flow has something real to exercise. Same caveat as the plan
-- seed: which promotions to actually run is a marketing decision, not a technical one.
INSERT INTO coupon (code, description, discount_type, discount_value, applicable_plan_code, max_redemptions, redemption_count, starts_at, expires_at, is_active, created_at, updated_at)
VALUES ('WELCOME10', '10% off any plan for new members', 'PERCENTAGE', 10.00, NULL, NULL, 0, NULL, NULL, TRUE, now(), now());
