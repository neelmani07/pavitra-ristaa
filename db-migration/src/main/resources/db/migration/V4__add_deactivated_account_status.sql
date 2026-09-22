-- Separates self-service deactivation from admin-driven suspension. Previously both used the SUSPENDED
-- status, so a future admin-suspended account could self-reactivate through POST /auth/reactivate.
-- DEACTIVATED is reversible by the account holder; SUSPENDED remains an admin action.

ALTER TABLE "user" DROP CONSTRAINT chk_user_account_status;

ALTER TABLE "user" ADD CONSTRAINT chk_user_account_status CHECK (account_status IN (
    'PENDING_VERIFICATION', 'ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'BLOCKED', 'DELETED'
));
