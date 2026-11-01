# OTP delivery provider — decision record

**Status:** Email delivery implemented (Brevo, 2026-11-01). SMS delivery (MSG91) not yet implemented. `LoggingOtpSender` (`api/src/main/java/com/pavitraristaa/auth/service/LoggingOtpSender.java`) still only logs that an OTP was dispatched, so registration/login-by-OTP cannot be completed outside a database-seeded test account.

## Recommendation

- **SMS (India): MSG91.** Use its Verify/OTP API rather than raw SMS sending.
- **Email: any transactional-email provider** (AWS SES, Postmark, Resend, SendGrid). Email has no DLT requirement, so this is a smaller, independent decision — start it in parallel, not blocked on SMS.
- Implement one new `OtpSender` (the existing interface in `api/src/main/java/com/pavitraristaa/auth/service/OtpSender.java`) per channel, selected by whether the destination is an email or a phone number, keeping `LoggingOtpSender` as the `local`/`test` profile implementation.

## Why MSG91 over Twilio Verify for the SMS leg

Both are legitimate. The deciding factor for this project is India's DLT (Distributed Ledger Technology) regulation, which TRAI requires for **all** commercial SMS to Indian numbers, transactional OTPs included:

- Every sender must register as a **Principal Entity** on a DLT platform, register a **sender/header ID**, and get the **exact message template** pre-approved. Unregistered SMS is silently dropped by Indian carriers, not just non-compliant.
- **MSG91** is an Indian-registered telecom entity and handles the DLT registration paperwork as part of onboarding — you fill in business documents through their portal and they submit it, rather than you dealing with a DLT operator (Airtel/Jio/Vodafone-Idea's DLT platforms, or an intermediary like Kaleyra) directly.
- **Twilio** is a global provider without that local registration relationship; using it for India still requires you to independently complete DLT registration and hand Twilio the approved template/sender ID. More steps, more places for the "who's actually responsible for this paperwork" question to stall.

If the product later expands outside India, Twilio Verify (or MSG91's own international coverage) is a reasonable second provider to add — the `OtpSender` interface is already the seam for that.

## Timeline: start DLT registration now

DLT template/sender approval commonly takes **3–10 business days**, sometimes longer for a first-time registration, and it blocks SMS delivery entirely until approved — nothing in the app can shortcut it. Concretely, in order:

1. Create an MSG91 account and start their DLT onboarding flow (business PAN/GST, authorized signatory details).
2. Register a sender/header ID (6 characters, e.g. `PAVITR`).
3. Register the OTP message template. Keep it close to the DLT-friendly boilerplate carriers expect, e.g.:
   `"<code> is your Pavitra Ristaa verification code. Valid for 10 minutes. Do not share this code with anyone."`
   Approval is against the *exact* template text with `{#var#}` placeholders — plan the copy once, since changing it later re-triggers approval.
4. Once approved, get the API key and wire `Msg91OtpSender` behind the existing `OtpSender` interface; configuration follows the same pattern as `MEDIA_*` — new env vars (`MSG91_AUTH_KEY`, `MSG91_SENDER_ID`, `MSG91_TEMPLATE_ID`), read via `PavitraProperties`.

## What I need from you to implement this

- An MSG91 (or chosen alternative) account, with the auth key and approved template/sender ID.
- The finalized OTP message copy (item 3 above), since it must be decided before submitting for DLT approval.
- Confirmation this recommendation is accepted, or a different provider if you have an existing relationship (e.g. an aggregator your org already uses).

Until then, local/dev accounts must keep being created directly in the database (see the smoke-test pattern used in this session) rather than through `/auth/register`.

## Update 2026-11-01: email OTPs go out through Brevo

- **What it does:** with `BREVO_API_KEY` and `BREVO_SENDER_EMAIL` set, every OTP and password-reset code addressed to an email is sent as a transactional email (`POST https://api.brevo.com/v3/smtp/email`, `api-key` header). Mobile numbers still only log ("OTP dispatched") until MSG91 is added; `OtpSenderConfig` is the single place to plug it in.
- **Not configured = logged, not sent.** A blank `BREVO_API_KEY` keeps today's behaviour, which is what every environment and every test uses. An API key without a sender address stops the app at startup on purpose.
- **To switch it on (Render):** in Brevo create an API key (SMTP & API > API Keys), add the sender address under Senders, Domains & Dedicated IPs and verify it, ideally authenticating the whole domain (SPF/DKIM) or mail lands in spam. Then set `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` and optionally `BREVO_SENDER_NAME` on the service and redeploy. Check the logs for `Brevo rejected` / `Could not reach Brevo` after the first registration.
- **A failed send never fails the request.** It is logged at ERROR (masked address, never the code) and the user can ask for a new one. The consequence is that a wrong key surfaces in the logs, not to the user.
- **Sent after the database commit,** not inside the transaction: a registration that rolls back never emails a code, and the HTTP call does not hold a database connection.
- **Throttled:** at most one OTP per destination and purpose every 60 seconds, and 5 per rolling hour (`pavitra.otp.resend-cooldown`, `pavitra.otp.max-per-hour`). A request over the limit is dropped silently and answered with the usual "sent if valid" success, so it cannot be used to discover which addresses have accounts. This also protects the Brevo daily quota (free plan: about 300 emails a day per third-party sources; confirm on Brevo's own pricing page) from being used up by someone hammering resend.
- **Password reset email** contains the recovery token itself, since there is no frontend reset-page URL configured yet. Adding `pavitra.frontend-base-url` and sending a link is the natural next step once the FE has the page.
