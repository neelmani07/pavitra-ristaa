# Staging vs. production — decision record

**Status:** Staging environment provisioned (2026-09-26). Production not yet set up.

## The rule

Staging and production never share a config file, a database, a bucket, or a set of provider
credentials. Concretely:

- **Spring profile**: `application-stage.yml` (staging) is a separate file from
  `application-prod.yml` (production) — one does not extend or import the other. A change made
  for staging (e.g. more verbose logging for active FE-integration debugging) can never leak into
  what production actually runs, because there's no inheritance between them to leak through.
- **Database**: staging uses its own Neon project (`pavitra ristaa`,
  `delicate-poetry-05510144`). Its default branch happens to be named `production` — that's Neon's
  own default branch name for a new project, not a claim about what it's used for; everything
  about this project (the `pavitra-media-staging` bucket name, the env vars it feeds) is staging
  only. Production gets its own separate Neon project (or at minimum a separate branch) when it's
  set up, specifically so a staging experiment can never touch real user data.
- **Object storage**: `pavitra-media-staging` bucket, private access (see media moderation note
  below). Production gets its own bucket.
- **Payment/OTP providers**: staging runs with the `AutoApprovePaymentGateway` stub
  (`RAZORPAY_KEY_ID` left blank) and `LoggingOtpSender` (OTP not yet live either environment - see
  `OTP_DELIVERY_PROVIDER.md`). Production will need real Razorpay keys and a real MSG91
  integration - neither should ever be pasted into staging's env vars, and vice versa.
- **CORS**: `CORS_ALLOWED_ORIGINS` is set per environment via Render's dashboard env vars, never
  baked into either YAML file - staging points at whatever URL the FE dev's staging deploy uses,
  production will point at the real domain.
- **Everything environment-specific comes from env vars set directly in the hosting dashboard**
  (Render), never committed to either YAML file - both `application-stage.yml` and
  `application-prod.yml` only reference `${VAR}` placeholders with no fallback default, so the
  app fails fast on boot if an expected var is missing rather than silently falling back to the
  wrong environment's value.

## Why this matters here specifically

The object-storage bucket is private specifically because pending/rejected profile photos must
never be publicly reachable (see the original hardening-pass note in the initial commit message).
Private-bucket + presigned-URL is an app-level guarantee, not a bucket-level one — the app is what
decides whether a URL should be issued. Keeping staging and production on entirely separate
buckets/credentials means a staging bug (or a staging credential that leaks, e.g. to a FE dev who
doesn't need write access to anything else) can't become a production data exposure.
