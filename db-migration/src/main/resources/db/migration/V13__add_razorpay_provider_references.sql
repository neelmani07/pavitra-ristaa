-- Adds the minimum needed to drive Razorpay Subscriptions (recurring, UPI Autopay / card e-mandate) rather than
-- a one-off charge per cycle:
--   - plan.provider_plan_id: Razorpay's own plan id, created once per our plan and cached here (lazily, on
--     first checkout - see RazorpayPaymentGateway.ensureProviderPlan()) so it isn't recreated every checkout.
--   - subscription.provider_subscription_id: Razorpay's subscription id, i.e. the recurring mandate itself.
--     Both are nullable: a subscription created via the stub gateway (dev/test, no real provider configured)
--     has none, and the schema doesn't assume every provider is Razorpay specifically - the column name says
--     "provider", not "razorpay", on purpose.
ALTER TABLE plan ADD COLUMN provider_plan_id VARCHAR(255);
ALTER TABLE subscription ADD COLUMN provider_subscription_id VARCHAR(255);

CREATE UNIQUE INDEX uq_subscription_provider_id ON subscription (provider_subscription_id)
    WHERE provider_subscription_id IS NOT NULL;
