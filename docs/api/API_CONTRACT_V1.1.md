# API CONTRACT V1.1
## Relationship Platform — MVP

**Scope:** Dating/Love + Friendship + Marriage. REST is primary; WebSocket is used for chat delivery. This contract is derived from the Product Knowledge Base and the approved v1.1 52-table schema.

## 1. API conventions
- Base path: `/api/v1`
- Authentication: Bearer JWT for authenticated endpoints.
- Public identifiers: UUIDs where the schema defines externally exposed UUIDs; internal BIGINT IDs remain internal.
- Time: UTC / ISO-8601 timestamps.
- JSON field naming: camelCase at API boundary; database remains snake_case.
- Pagination: `page` (0-based) + `size`; default size 20, maximum 100.
- API is capability-oriented; database tables are not exposed as CRUD resources.
- Chat delivery uses WebSocket; REST provides conversation/message history and management.

## 2. Endpoint inventory
### Authentication
- `POST /auth/register` — Register a new account
- `POST /auth/verify-otp` — Verify an OTP
- `POST /auth/resend-otp` — Resend an OTP
- `POST /auth/verify-email` — Verify email address
- `POST /auth/login` — Authenticate with password
- `POST /auth/login/otp` — Authenticate using mobile OTP
- `POST /auth/oauth/google` — Authenticate or register with Google
- `POST /auth/oauth/apple` — Authenticate or register with Apple
- `POST /auth/refresh` — Refresh access token
- `POST /auth/logout` — Logout current session
- `POST /auth/logout-all` — Logout all active sessions
- `POST /auth/forgot-password` — Request password recovery
- `POST /auth/reset-password` — Reset password using recovery token or OTP
- `POST /auth/change-password` — Change current password
- `GET /auth/sessions` — List active sessions
- `DELETE /auth/sessions/{sessionId}` — Revoke a specific session
- `GET /auth/linked-accounts` — List linked login methods
- `POST /auth/linked-accounts/{provider}` — Link a Google or Apple account
- `DELETE /auth/linked-accounts/{provider}` — Unlink a linked login method
- `GET /auth/me` — Get authenticated account
- `POST /auth/deactivate` — Deactivate account
- `POST /auth/reactivate` — Reactivate a deactivated account
- `POST /auth/delete` — Request account deletion
- `POST /auth/data-export` — Request personal data export

### Profile
- `GET /me/profile` — Get my complete profile
- `PUT /me/profile` — Update core profile information
- `GET /me/profile/education` — Get education details
- `PUT /me/profile/education` — Update education details
- `GET /me/profile/career` — Get career details
- `PUT /me/profile/career` — Update career details
- `GET /me/profile/family` — Get family details
- `PUT /me/profile/family` — Update family details
- `GET /me/profile/lifestyle` — Get lifestyle details
- `PUT /me/profile/lifestyle` — Update lifestyle details
- `GET /me/profile/spiritual` — Get optional spiritual profile
- `PUT /me/profile/spiritual` — Create or replace optional spiritual profile
- `GET /me/profile/languages` — List profile languages
- `PUT /me/profile/languages` — Replace profile languages
- `GET /me/profile/interests` — List profile interests
- `PUT /me/profile/interests` — Replace profile interests
- `GET /me/profile/hobbies` — List profile hobbies
- `PUT /me/profile/hobbies` — Replace profile hobbies
- `GET /me/profile/photos` — List my profile photos
- `POST /me/profile/photos` — Create profile photo metadata after upload
- `DELETE /me/profile/photos/{photoId}` — Delete a profile photo
- `PUT /me/profile/photos/{photoId}` — Update profile photo metadata
- `POST /me/profile/publish` — Publish profile
- `POST /me/profile/unpublish` — Unpublish profile
- `GET /me/profile/preview` — Preview public profile representation
- `GET /me/profile/completion` — Get profile completion information
- `GET /profiles/{profileId}` — Get another member profile

### Relationship Modes
- `GET /relationship-modes` — List active relationship modes
- `GET /me/relationship-modes` — Get my relationship modes
- `PUT /me/relationship-modes` — Replace my relationship modes

### Master Data
- `GET /master-data/{categoryCode}` — List active values for a master-data category
- `GET /master-data/categories` — List master-data categories

### Partner Preferences
- `GET /me/preferences` — Get partner preferences
- `PUT /me/preferences` — Replace partner preferences

### Discovery
- `GET /home` — Get personalized home discovery payload
- `GET /discovery/profiles` — Browse discoverable profiles
- `POST /discovery/search` — Search profiles using filters
- `GET /discovery/recommendations` — Get persisted recommendations
- `POST /discovery/recommendations/refresh` — Refresh recommendation set
- `GET /discovery/collections` — List discovery collections
- `GET /discovery/collections/{collectionId}` — Get collection details and members
- `GET /discovery/saved-searches` — List saved searches
- `POST /discovery/saved-searches` — Create saved search
- `PUT /discovery/saved-searches/{searchId}` — Update saved search
- `DELETE /discovery/saved-searches/{searchId}` — Delete saved search
- `GET /discovery/search-history` — List recent search history
- `DELETE /discovery/search-history` — Clear search history
- `GET /discovery/recently-viewed` — List recently viewed profiles
- `POST /discovery/profiles/{profileId}/view` — Explicitly record a profile view
- `GET /discovery/preferences` — Get discovery preferences
- `PUT /discovery/preferences` — Update discovery preferences

Spiritual discovery fields are optional and text-based: `spiritualCommunity`, `spiritualInterest`, `practice`, and `anySpiritualProfession`. `dreamSpiritualPilgrimageDestination` is profile information, not a required discovery filter.

### Favorites
- `GET /favorites` — List saved/favorited profiles
- `POST /favorites/{profileId}` — Add profile to favorites
- `DELETE /favorites/{profileId}` — Remove profile from favorites
- `GET /shortlist` — List shortlisted profiles
- `POST /shortlist/{profileId}` — Add profile to shortlist
- `DELETE /shortlist/{profileId}` — Remove profile from shortlist

### Connections
- `POST /interests` — Send an interest
- `GET /interests/sent` — List sent interests
- `GET /interests/received` — List received interests
- `GET /interests/{interestId}` — Get interest details
- `POST /interests/{interestId}/accept` — Accept received interest
- `POST /interests/{interestId}/decline` — Decline received interest
- `POST /interests/{interestId}/withdraw` — Withdraw sent interest
- `GET /matches` — List active matches
- `GET /matches/history` — List match history
- `GET /matches/{matchId}` — Get match details
- `POST /matches/{matchId}/unmatch` — End a match
- `GET /matches/{matchId}/compatibility` — Get compatibility information
- `GET /matches/{matchId}/ice-breakers` — Get suggested conversation starters

### Messaging
- `GET /conversations` — List conversations
- `GET /conversations/{conversationId}` — Get conversation details
- `PUT /conversations/{conversationId}` — Update conversation settings/status
- `GET /conversations/{conversationId}/messages` — Get paginated message history
- `GET /conversations/{conversationId}/messages/{messageId}` — Get one message
- `DELETE /conversations/{conversationId}/messages/{messageId}` — Delete a message
- `POST /conversations/{conversationId}/messages/{messageId}/read` — Mark message as read
- `POST /conversations/{conversationId}/read` — Mark conversation messages as read
- `PUT /conversations/{conversationId}/messages/{messageId}/reaction` — Add or replace a message reaction
- `GET /conversations/{conversationId}/media` — List shared media
- `POST /conversations/{conversationId}/report` — Report conversation/user from chat
- `POST /conversations/{conversationId}/block` — Block participant from chat

### Notifications
- `GET /notifications` — List notifications
- `GET /notifications/{notificationId}` — Get notification details
- `DELETE /notifications/{notificationId}` — Delete notification
- `POST /notifications/{notificationId}/read` — Mark notification as read
- `POST /notifications/read-all` — Mark all notifications as read
- `GET /notification-settings` — Get notification preferences
- `PUT /notification-settings` — Update notification preferences

### Media
- `POST /media/upload-url` — Create a presigned upload URL
- `POST /media/complete` — Complete a media upload
- `DELETE /media/{mediaId}` — Delete owned media

### Trust & Safety
- `GET /verification/status` — Get current verification status
- `POST /verification/requests` — Submit verification request
- `GET /verification/{verificationId}` — Get verification status/details
- `GET /blocks` — List blocked users
- `POST /blocks/{userId}` — Block a user
- `DELETE /blocks/{userId}` — Unblock a user
- `GET /reports/reasons` — List active report reasons
- `POST /reports` — Report a user, profile or message
- `GET /safety-center` — Get safety center content
- `POST /appeals` — Submit an appeal

### Subscription & Payments
- `GET /plans` — List active subscription plans
- `GET /plans/{planCode}` — Get plan details
- `GET /subscriptions/me` — Get current subscription
- `POST /subscriptions` — Start subscription checkout
- `GET /subscriptions/{subscriptionId}` — Get subscription details
- `POST /subscriptions/{subscriptionId}/cancel` — Cancel subscription
- `PUT /subscriptions/{subscriptionId}/auto-renew` — Enable or disable auto-renewal
- `GET /payments` — List payment history
- `GET /payments/{paymentId}` — Get payment status/details
- `POST /payments/{paymentId}/retry` — Retry failed payment
- `GET /invoices` — List invoices
- `GET /invoices/{invoiceId}` — Get invoice details
- `POST /coupons/validate` — Validate a coupon

### Settings & Privacy
- `GET /me/settings` — Get account settings
- `PUT /me/settings` — Update account settings
- `GET /me/privacy` — Get profile privacy settings
- `PUT /me/privacy` — Update profile privacy settings
- `GET /me/communication-settings` — Get communication settings
- `PUT /me/communication-settings` — Update communication settings
- `GET /me/appearance` — Get appearance settings
- `PUT /me/appearance` — Update appearance settings
- `GET /me/data-privacy` — Get data/privacy controls
- `POST /me/data-privacy/export` — Request data export
- `POST /me/account/email` — Request email change
- `POST /me/account/mobile` — Request mobile change
- `POST /me/security/re-authenticate` — Re-authenticate for sensitive action

### Support
- `GET /support/tickets` — List my support tickets
- `POST /support/tickets` — Create support ticket
- `GET /support/tickets/{ticketId}` — Get support ticket
- `PUT /support/tickets/{ticketId}` — Update support ticket from user side
- `GET /help` — Get help center content
- `GET /legal/{documentType}` — Get a legal document

### Admin
- `GET /admin/users` — List/search users
- `GET /admin/users/{userId}` — Get user details
- `PUT /admin/users/{userId}` — Update user account details
- `POST /admin/users/{userId}/suspend` — Suspend user
- `POST /admin/users/{userId}/activate` — Activate user
- `POST /admin/users/{userId}/delete` — Delete user account
- `GET /admin/users/{userId}/login-history` — View user login history
- `GET /admin/verifications` — List verification cases
- `POST /admin/verifications/{verificationId}/approve` — Approve verification
- `POST /admin/verifications/{verificationId}/reject` — Reject verification
- `GET /admin/reports` — List reports
- `GET /admin/reports/{reportId}` — Get report details
- `POST /admin/reports/{reportId}/resolve` — Resolve report
- `GET /admin/moderation` — List moderation cases
- `POST /admin/moderation/{moderationId}/resolve` — Resolve moderation case
- `GET /admin/support/tickets` — List support tickets
- `POST /admin/support/tickets/{ticketId}/assign` — Assign support ticket
- `POST /admin/support/tickets/{ticketId}/resolve` — Resolve support ticket
- `GET /admin/audit-logs` — List audit logs
- `GET /admin/settings` — List platform settings
- `PUT /admin/settings` — Update platform setting

### Analytics & Growth
- `GET /admin/analytics/users` — User analytics
- `GET /admin/analytics/profiles` — Profile analytics
- `GET /admin/analytics/matching` — Matching analytics
- `GET /admin/analytics/revenue` — Revenue analytics

## 3. Core business rules captured from source
- Registration supports email, mobile OTP, Google and Apple; email/mobile uniqueness, verified contact requirement, minimum age, terms/privacy acceptance, and duplicate-account prevention are product rules.
- Login supports email/mobile/password/OTP/Google/Apple, multiple devices, login history, and account-state checks.
- Users may select multiple relationship intentions/modes; the approved schema models DATING, FRIENDSHIP and MARRIAGE through `relationship_mode` and `user_relationship_mode`.
- Relationship mode represents user intent/context only. Partner preferences and compatibility use one shared parameter set across DATING, FRIENDSHIP and MARRIAGE; there are no mode-specific partner-preference records.
- The optional `spiritual_profile` contains only lightweight spiritual information. All user-facing spiritual fields are optional, and absence of spiritual information is valid and should not count as a negative compatibility signal.
- Discovery supports search/filtering by age, location, profession, education, communities, languages, interests, relationship intent and other product filters; recommendation snapshots are persisted while recommendation logic remains application logic.
- Interest flow is send → received → accept/decline; acceptance creates a match, followed by conversation.
- Chat is one-to-one for MVP, with message history over REST and live delivery over WebSocket.
- Notifications cover relationship, messaging, profile, subscription, community, security and platform categories.
- Subscription schema supports plans, subscriptions, payments and invoices; product requirements include coupons, auto-renewal and refunds, but some provider-specific payment behavior is not defined by the schema.

## 4. Profile and compatibility alignment
- `user_profile` remains the core profile record; `spiritual_profile` is an optional 1:1 extension.
- Spiritual profile fields: `spiritualCommunity`, `spiritualInterests`, `practices`, `anySpiritualProfession`, `dreamSpiritualPilgrimageDestination`. No spiritual path, spiritual role, years-of-practice, teacher flag, or volunteering field is required by the MVP contract.
- `GET /me/profile` and member-profile responses may include `spiritualProfile: null` when no spiritual information has been provided.
- `PUT /me/profile/spiritual` accepts all fields as optional and nullable; omitted/empty spiritual information does not make profile creation or profile matching invalid.
- `/me/preferences` is the single partner-preference resource. The previous mode-specific preference endpoints are removed.
- `/matches/{matchId}/compatibility` uses the same underlying profile/preference attributes for Dating, Friendship and Marriage. Relationship mode is context, not a different compatibility model.

## 5. WebSocket chat contract
- Endpoint: `/ws/chat` (proposed transport endpoint).
- Authentication: JWT during WebSocket connection establishment.
- Client send message payload: `conversationId`, `clientMessageId`, `messageType`, `content`, optional `replyToMessageId`, optional attachment IDs.
- Server events: `MESSAGE_SENT`, `MESSAGE_DELIVERED`, `MESSAGE_READ`, `MESSAGE_EDITED`, `MESSAGE_DELETED`, `REACTION_UPDATED`, `SYSTEM`.
- Exact STOMP destinations/frames are implementation details and should be finalized before chat implementation.

## 6. Status / implementation readiness
- **Ready for implementation:** Authentication, core profile including optional spiritual profile, relationship modes, shared partner preferences, discovery/search, favorites/shortlist mapping, interests, matches, blocks/reports, chat persistence/history, notifications, media metadata, subscriptions/payments/invoices, support tickets, moderation/audit/system settings.
- **Schema decision required:** user privacy/settings, appearance, communication settings, data-consent/export persistence, and other account preferences not represented by the approved 52 tables.
- **Deferred from MVP contract:** Community domain (events, groups, discussions, retreats, volunteer activities, directory, resources) because the approved MVP schema contains no community-domain tables. The Product Knowledge Base contains these capabilities, but the database schema does not currently support them.
- **Analytics decision required:** product analytics, referrals, success stories, feedback/surveys, campaigns and experiments are defined in the Product Knowledge Base but have no corresponding event/analytics tables in the approved schema.

## 7. Error code baseline
- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `RESOURCE_NOT_FOUND`
- `DUPLICATE_RESOURCE`
- `ACCOUNT_NOT_VERIFIED`
- `ACCOUNT_SUSPENDED`
- `ACCOUNT_DELETED`
- `OTP_INVALID`
- `OTP_EXPIRED`
- `TOO_MANY_ATTEMPTS`
- `INVALID_CREDENTIALS`
- `SESSION_EXPIRED`
- `SESSION_REVOKED`
- `PROFILE_NOT_ACTIVE`
- `CANNOT_INTERACT_WITH_SELF`
- `USER_BLOCKED`
- `ALREADY_INTERESTED`
- `INTEREST_NOT_ACTIONABLE`
- `MATCH_NOT_FOUND`
- `CONVERSATION_NOT_FOUND`
- `MESSAGE_NOT_FOUND`
- `MEDIA_NOT_FOUND`
- `PAYMENT_FAILED`
- `SUBSCRIPTION_NOT_ACTIVE`
- `PLAN_NOT_FOUND`
- `REPORT_NOT_FOUND`
- `VERIFICATION_NOT_FOUND`
- `SUPPORT_TICKET_NOT_FOUND`
- `RATE_LIMITED`
- `INTERNAL_ERROR`

## 8. Frontend handoff
1. Import `openapi-v1.1.yaml` into Swagger Editor/Postman or generate a TypeScript client from it.
2. Use the endpoint inventory as the frontend service/module boundary.
3. Backend implementation should preserve paths, request/response contracts and error codes unless a contract change is explicitly agreed.
4. Once Spring Boot is running, expose live Swagger UI and `/v3/api-docs`; the YAML remains the versioned contract.

### Example spiritual profile payload
```json
{
  "spiritualCommunity": "Isha",
  "spiritualInterests": "Meditation, Yoga",
  "practices": "Hatha Yoga",
  "anySpiritualProfession": null,
  "dreamSpiritualPilgrimageDestination": "Kailash"
}
```
