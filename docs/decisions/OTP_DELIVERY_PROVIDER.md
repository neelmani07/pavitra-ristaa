# OTP delivery provider — decision record

**Status:** Recommendation made; not yet implemented. `LoggingOtpSender` (`api/src/main/java/com/pavitraristaa/auth/service/LoggingOtpSender.java`) still only logs that an OTP was dispatched, so registration/login-by-OTP cannot be completed outside a database-seeded test account.

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
