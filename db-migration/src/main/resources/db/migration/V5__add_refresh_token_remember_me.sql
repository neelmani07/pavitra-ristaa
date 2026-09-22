-- Fixes a bug: rotating a refresh token (POST /auth/refresh) always issued the next one with the short
-- (30-day) TTL, even for a session that started with "remember me" (90-day). Nothing recorded which TTL a
-- session was meant to keep, so a remember-me session silently dropped to 30 days after its first refresh.

ALTER TABLE refresh_token ADD COLUMN remember_me BOOLEAN NOT NULL DEFAULT FALSE;
