package com.pavitraristaa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pavitraristaa.admin.dto.AdminAppealResponse;
import com.pavitraristaa.admin.dto.AdminModerationResponse;
import com.pavitraristaa.admin.dto.AdminReportResponse;
import com.pavitraristaa.admin.dto.AdminSettingResponse;
import com.pavitraristaa.admin.dto.AdminSupportTicketResponse;
import com.pavitraristaa.admin.dto.AdminUserResponse;
import com.pavitraristaa.admin.dto.AdminVerificationResponse;
import com.pavitraristaa.admin.dto.AssignTicketRequest;
import com.pavitraristaa.admin.dto.AuditLogResponse;
import com.pavitraristaa.admin.dto.CreateModerationRequest;
import com.pavitraristaa.admin.dto.LoginHistoryResponse;
import com.pavitraristaa.admin.dto.ResolveAppealRequest;
import com.pavitraristaa.admin.dto.ResolveModerationRequest;
import com.pavitraristaa.admin.dto.ResolveReportRequest;
import com.pavitraristaa.admin.dto.ResolveTicketRequest;
import com.pavitraristaa.admin.dto.SuspendUserRequest;
import com.pavitraristaa.admin.dto.UpdateSettingRequest;
import com.pavitraristaa.admin.dto.UpdateUserRolesRequest;
import com.pavitraristaa.admin.dto.VerificationDecisionRequest;
import com.pavitraristaa.admin.service.AdminAppealService;
import com.pavitraristaa.admin.service.AdminModerationService;
import com.pavitraristaa.admin.service.AdminReportService;
import com.pavitraristaa.admin.service.AdminSettingService;
import com.pavitraristaa.admin.service.AdminSupportService;
import com.pavitraristaa.admin.service.AdminUserService;
import com.pavitraristaa.admin.service.AdminVerificationService;
import com.pavitraristaa.admin.service.AuditLogService;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.connections.dto.CreateInterestRequest;
import com.pavitraristaa.connections.dto.InterestResponse;
import com.pavitraristaa.connections.dto.MatchResponse;
import com.pavitraristaa.connections.service.InterestService;
import com.pavitraristaa.connections.service.MatchService;
import com.pavitraristaa.discovery.dto.CollectionDetailResponse;
import com.pavitraristaa.discovery.dto.CollectionSummaryResponse;
import com.pavitraristaa.discovery.dto.CreateSavedSearchRequest;
import com.pavitraristaa.discovery.dto.DiscoverySearchRequest;
import com.pavitraristaa.discovery.dto.HomeResponse;
import com.pavitraristaa.discovery.dto.SavedSearchResponse;
import com.pavitraristaa.discovery.dto.SearchHistoryResponse;
import com.pavitraristaa.discovery.dto.UpdateSavedSearchRequest;
import com.pavitraristaa.discovery.service.DiscoveryCollectionService;
import com.pavitraristaa.discovery.service.DiscoveryService;
import com.pavitraristaa.discovery.service.RecommendationService;
import com.pavitraristaa.discovery.service.SavedSearchService;
import com.pavitraristaa.discovery.service.SearchHistoryService;
import com.pavitraristaa.favorites.service.FavoriteService;
import com.pavitraristaa.master.dto.MasterValueResponse;
import com.pavitraristaa.master.service.MasterDataService;
import com.pavitraristaa.media.dto.CompleteUploadRequest;
import com.pavitraristaa.media.service.MediaService;
import com.pavitraristaa.messaging.dto.ConversationResponse;
import com.pavitraristaa.messaging.dto.MessageResponse;
import com.pavitraristaa.messaging.dto.ReactionRequest;
import com.pavitraristaa.messaging.service.ConversationService;
import com.pavitraristaa.messaging.service.MessageService;
import com.pavitraristaa.notifications.dto.NotificationResponse;
import com.pavitraristaa.notifications.dto.NotificationSettingResponse;
import com.pavitraristaa.notifications.dto.NotificationSettingUpdate;
import com.pavitraristaa.notifications.dto.UpdateNotificationSettingsRequest;
import com.pavitraristaa.notifications.entity.NotificationType;
import com.pavitraristaa.notifications.service.NotificationService;
import com.pavitraristaa.notifications.service.NotificationSettingService;
import com.pavitraristaa.preference.dto.PartnerPreferenceRequest;
import com.pavitraristaa.preference.dto.PartnerPreferenceResponse;
import com.pavitraristaa.preference.dto.PreferenceValueRequest;
import com.pavitraristaa.preference.service.PartnerPreferenceService;
import com.pavitraristaa.profile.dto.LanguageItemRequest;
import com.pavitraristaa.profile.dto.ReplaceHobbiesRequest;
import com.pavitraristaa.profile.dto.ReplaceInterestsRequest;
import com.pavitraristaa.profile.dto.ReplaceLanguagesRequest;
import com.pavitraristaa.profile.dto.UpdatePhotoRequest;
import com.pavitraristaa.profile.dto.UpdateProfileRequest;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.service.ProfileService;
import com.pavitraristaa.subscriptions.dto.CouponValidationResponse;
import com.pavitraristaa.subscriptions.dto.InvoiceResponse;
import com.pavitraristaa.subscriptions.dto.PaymentResponse;
import com.pavitraristaa.subscriptions.dto.PlanResponse;
import com.pavitraristaa.subscriptions.dto.StartSubscriptionRequest;
import com.pavitraristaa.subscriptions.dto.SubscriptionResponse;
import com.pavitraristaa.subscriptions.dto.UpdateAutoRenewRequest;
import com.pavitraristaa.subscriptions.dto.ValidateCouponRequest;
import com.pavitraristaa.subscriptions.service.CouponService;
import com.pavitraristaa.subscriptions.service.InvoiceService;
import com.pavitraristaa.subscriptions.service.PaymentService;
import com.pavitraristaa.subscriptions.service.PlanService;
import com.pavitraristaa.subscriptions.service.SubscriptionService;
import com.pavitraristaa.support.dto.CreateSupportTicketRequest;
import com.pavitraristaa.support.dto.SupportTicketResponse;
import com.pavitraristaa.support.dto.UpdateSupportTicketRequest;
import com.pavitraristaa.support.service.HelpContentService;
import com.pavitraristaa.support.service.SupportTicketService;
import com.pavitraristaa.trust.dto.AppealResponse;
import com.pavitraristaa.trust.dto.CreateReportRequest;
import com.pavitraristaa.trust.dto.ReportResponse;
import com.pavitraristaa.trust.dto.SubmitAppealRequest;
import com.pavitraristaa.trust.dto.SubmitVerificationRequest;
import com.pavitraristaa.trust.dto.VerificationStatusResponse;
import com.pavitraristaa.trust.service.AppealService;
import com.pavitraristaa.trust.service.BlockService;
import com.pavitraristaa.trust.service.ReportService;
import com.pavitraristaa.trust.service.VerificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises services against a real PostgreSQL with the Flyway migrations applied. Subclasses supply the database.
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractPersistenceTests {

    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProfileService profileService;
    @Autowired private PartnerPreferenceService partnerPreferenceService;
    @Autowired private MasterDataService masterDataService;
    @Autowired private MediaService mediaService;
    @Autowired private FavoriteService favoriteService;
    @Autowired private DiscoveryService discoveryService;
    @Autowired private InterestService interestService;
    @Autowired private MatchService matchService;
    @Autowired private ConversationService conversationService;
    @Autowired private MessageService messageService;
    @Autowired private BlockService blockService;
    @Autowired private ReportService reportService;
    @Autowired private VerificationService verificationService;
    @Autowired private SupportTicketService supportTicketService;
    @Autowired private HelpContentService helpContentService;
    @Autowired private AdminUserService adminUserService;
    @Autowired private AdminVerificationService adminVerificationService;
    @Autowired private AdminReportService adminReportService;
    @Autowired private AdminModerationService adminModerationService;
    @Autowired private AdminSupportService adminSupportService;
    @Autowired private AdminSettingService adminSettingService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationSettingService notificationSettingService;
    @Autowired private RecommendationService recommendationService;
    @Autowired private SavedSearchService savedSearchService;
    @Autowired private SearchHistoryService searchHistoryService;
    @Autowired private DiscoveryCollectionService discoveryCollectionService;
    @Autowired private AppealService appealService;
    @Autowired private AdminAppealService adminAppealService;
    @Autowired private PlanService planService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private PaymentService paymentService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private CouponService couponService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void seededMasterDataIsServedThroughTheApiService() {
        PagedData<MasterValueResponse> diets = masterDataService.values("DIET", null, PageRequest.of(0, 50));
        PagedData<MasterValueResponse> countries = masterDataService.values("COUNTRY", "india", PageRequest.of(0, 50));

        assertThat(diets.items()).extracting(MasterValueResponse::code).contains("VEGETARIAN", "VEGAN", "JAIN");
        assertThat(countries.items()).extracting(MasterValueResponse::code).containsExactly("IN");
    }

    @Test
    void replacingInterestsWithAnOverlappingSetSucceeds() {
        AuthenticatedUser me = userWithProfile();
        long yoga = id("INTEREST", "YOGA");
        long meditation = id("INTEREST", "MEDITATION");
        long travel = id("INTEREST", "TRAVEL");

        profileService.replaceInterests(me, new ReplaceInterestsRequest(List.of(yoga, meditation)));
        var result = profileService.replaceInterests(me, new ReplaceInterestsRequest(List.of(meditation, travel)));

        assertThat(result).hasSize(2);
        assertThat(profileService.getInterests(me)).hasSize(2);
    }

    @Test
    void replacingHobbiesWithAnOverlappingSetSucceeds() {
        AuthenticatedUser me = userWithProfile();
        long reading = id("HOBBY", "READING");
        long chess = id("HOBBY", "CHESS");

        profileService.replaceHobbies(me, new ReplaceHobbiesRequest(List.of(reading)));
        var result = profileService.replaceHobbies(me, new ReplaceHobbiesRequest(List.of(reading, chess)));

        assertThat(result).hasSize(2);
    }

    @Test
    void replacingLanguagesWithAnOverlappingSetSucceeds() {
        AuthenticatedUser me = userWithProfile();
        long english = id("LANGUAGE", "ENGLISH");
        long hindi = id("LANGUAGE", "HINDI");

        profileService.replaceLanguages(me, new ReplaceLanguagesRequest(List.of(
                new LanguageItemRequest(english, "FLUENT", true))));
        var result = profileService.replaceLanguages(me, new ReplaceLanguagesRequest(List.of(
                new LanguageItemRequest(english, "NATIVE", true),
                new LanguageItemRequest(hindi, "FLUENT", false))));

        assertThat(result).hasSize(2);
    }

    @Test
    void partnerPreferencesRoundTripAndReplaceKeepingSomeValues() {
        AuthenticatedUser me = userWithProfile();
        assertThat(partnerPreferenceService.getMine(me).values()).isEmpty();

        PartnerPreferenceResponse saved = partnerPreferenceService.replaceMine(me, new PartnerPreferenceRequest(
                25, 35, 160, 185, "FEMALE",
                new BigDecimal("500000"), new BigDecimal("1500000"), "inr",
                id("MARITAL_STATUS", "NEVER_MARRIED"), 50,
                List.of(
                        value("DIET", "VEGETARIAN"),
                        value("DIET", "VEGAN"),
                        value("LANGUAGE", "HINDI"))));

        assertThat(saved.minAge()).isEqualTo(25);
        assertThat(saved.currencyCode()).isEqualTo("INR");
        assertThat(saved.values()).hasSize(3);
        assertThat(partnerPreferenceService.getMine(me)).isEqualTo(saved);

        PartnerPreferenceResponse replaced = partnerPreferenceService.replaceMine(me, new PartnerPreferenceRequest(
                null, null, null, null, null, null, null, null, null, null,
                List.of(
                        value("DIET", "VEGETARIAN"),
                        value("DIET", "VEGETARIAN"),
                        value("LANGUAGE", "ENGLISH"))));

        assertThat(replaced.minAge()).isNull();
        assertThat(replaced.preferredMaritalStatusId()).isNull();
        assertThat(replaced.values())
                .extracting(v -> v.preferenceType() + ":" + v.code())
                .containsExactlyInAnyOrder("DIET:VEGETARIAN", "LANGUAGE:ENGLISH");
        assertThat(partnerPreferenceService.replaceMine(me, new PartnerPreferenceRequest(
                null, null, null, null, null, null, null, null, null, null, null)).values()).isEmpty();
    }

    @Test
    void preferenceValueMustBelongToItsType() {
        AuthenticatedUser me = userWithProfile();
        PreferenceValueRequest religionAsDiet =
                new PreferenceValueRequest("DIET", id("RELIGION", "HINDUISM"));

        assertThatThrownBy(() -> partnerPreferenceService.replaceMine(me, new PartnerPreferenceRequest(
                null, null, null, null, null, null, null, null, null, null, List.of(religionAsDiet))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void preferencesRequireAProfile() {
        AuthenticatedUser me = principalFor(newUser());

        assertThatThrownBy(() -> partnerPreferenceService.getMine(me))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // --- Cross-user authorization: one user must never read/edit/delete another user's data. ---

    @Test
    void userOnlyEverSeesTheirOwnPartnerPreferences() {
        AuthenticatedUser userA = userWithProfile();
        AuthenticatedUser userB = userWithProfile();
        partnerPreferenceService.replaceMine(userA, new PartnerPreferenceRequest(
                25, 35, null, null, null, null, null, null, null, null, List.of(value("DIET", "VEGETARIAN"))));

        PartnerPreferenceResponse seenByB = partnerPreferenceService.getMine(userB);

        assertThat(seenByB.minAge()).isNull();
        assertThat(seenByB.values()).isEmpty();
    }

    @Test
    void userCannotCompleteOrDeleteAnotherUsersMedia() {
        AuthenticatedUser userA = userWithProfile();
        AuthenticatedUser userB = userWithProfile();
        UUID mediaOwnedByA = insertMedia(userA, "UPLOADING");

        assertThatThrownBy(() -> mediaService.complete(userB, new CompleteUploadRequest(mediaOwnedByA, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_FOUND));
        assertThatThrownBy(() -> mediaService.delete(userB, mediaOwnedByA))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_FOUND));
        assertThat(jdbcTemplate.queryForObject(
                "select status from media_file where uuid = ?", String.class, mediaOwnedByA))
                .isEqualTo("UPLOADING");
    }

    @Test
    void userCannotUpdateOrDeleteAnotherUsersProfilePhoto() {
        AuthenticatedUser userA = userWithProfile();
        AuthenticatedUser userB = userWithProfile();
        UUID mediaOwnedByA = insertMedia(userA, "ACTIVE");
        UUID photoOwnedByA = insertProfilePhoto(userA, mediaOwnedByA);

        assertThatThrownBy(() -> profileService.updatePhoto(
                userB, photoOwnedByA, new UpdatePhotoRequest(null, null, true, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThatThrownBy(() -> profileService.deletePhoto(userB, photoOwnedByA))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from profile_photo where uuid = ?", Integer.class, photoOwnedByA))
                .isEqualTo(1);

        // Also proves UserProfileRepository's entity graph (eager-fetches "photos") returns correctly for a
        // profile that actually owns one - not just the zero-photo case every other test happens to exercise.
        assertThat(profileService.getMine(userA).photos()).extracting(p -> p.id()).containsExactly(photoOwnedByA);
    }

    @Test
    void publicProfileViewRespectsProfileStatusAndOwnership() {
        AuthenticatedUser userA = userWithProfile();
        AuthenticatedUser userB = userWithProfile();
        UUID userAProfileId = userA.uuid();

        // A has not published yet (still DRAFT): a stranger gets RESOURCE_NOT_FOUND, not the draft data.
        assertThatThrownBy(() -> profileService.getPublic(userB, userAProfileId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        // A can always view their own profile through this endpoint too, regardless of status.
        assertThat(profileService.getPublic(userA, userAProfileId).id()).isEqualTo(userAProfileId);
    }

    private UUID insertMedia(AuthenticatedUser owner, String status) {
        UUID mediaUuid = UUID.randomUUID();
        Long ownerId = jdbcTemplate.queryForObject("select id from \"user\" where uuid = ?", Long.class, owner.uuid());
        jdbcTemplate.update(
                "insert into media_file (uuid, owner_user_id, storage_provider, bucket, object_key, mime_type, "
                        + "file_size_bytes, status, created_at) values (?, ?, 'MINIO', 'test-bucket', ?, 'image/jpeg', 2048, ?, now())",
                mediaUuid, ownerId, "test/" + mediaUuid, status);
        return mediaUuid;
    }

    private UUID insertProfilePhoto(AuthenticatedUser owner, UUID mediaUuid) {
        UUID photoUuid = UUID.randomUUID();
        Long profileId = jdbcTemplate.queryForObject(
                "select p.id from user_profile p join \"user\" u on u.id = p.user_id where u.uuid = ?",
                Long.class, owner.uuid());
        Long mediaId = jdbcTemplate.queryForObject("select id from media_file where uuid = ?", Long.class, mediaUuid);
        jdbcTemplate.update(
                "insert into profile_photo (uuid, profile_id, media_file_id, photo_type, display_order, is_primary, "
                        + "visibility, approval_status, created_at, updated_at) "
                        + "values (?, ?, ?, 'PROFILE', 0, true, 'PUBLIC', 'APPROVED', now(), now())",
                photoUuid, profileId, mediaId);
        return photoUuid;
    }

    private PreferenceValueRequest value(String type, String code) {
        String category = type;
        return new PreferenceValueRequest(type, id(category, code));
    }

    private long id(String category, String code) {
        return jdbcTemplate.queryForObject(
                "select v.id from master_value v join master_category c on c.id = v.category_id "
                        + "where c.code = ? and v.code = ?", Long.class, category, code);
    }

    private AuthenticatedUser userWithProfile() {
        AuthenticatedUser me = principalFor(newUser());
        profileService.updateCore(me, new UpdateProfileRequest(
                "Asha", null, null, LocalDate.now().minusYears(30), "FEMALE", null, null, null, null, null));
        return me;
    }

    private UserAccount newUser() {
        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setUuid(UUID.randomUUID());
        user.setEmail(user.getUuid() + "@example.test");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setVersion(0L);
        return userAccountRepository.save(user);
    }

    private AuthenticatedUser principalFor(UserAccount user) {
        return new AuthenticatedUser(user.getId(), user.getUuid(), List.of("USER"), 1L);
    }

    // --- Discovery / favorites / connections: a discoverable user needs a published (ACTIVE) profile and at
    // least one relationship mode - built directly via SQL here so each test controls exactly the age/gender/mode
    // it needs, without going through every profile-completeness requirement that real publish() enforces
    // (already covered by ProfileServiceTest/ProfileMapperTest).

    private AuthenticatedUser discoverableUser(String gender, int age, String modeCode) {
        AuthenticatedUser me = principalFor(newUser());
        profileService.updateCore(me, new UpdateProfileRequest(
                "Test", null, null, LocalDate.now().minusYears(age), gender, null, null, null, null, null));
        activateProfile(me);
        assignRelationshipMode(me, modeCode);
        return me;
    }

    private void activateProfile(AuthenticatedUser user) {
        jdbcTemplate.update(
                "update user_profile set profile_status = 'ACTIVE' "
                        + "where user_id = (select id from \"user\" where uuid = ?)",
                user.uuid());
    }

    private void assignRelationshipMode(AuthenticatedUser user, String modeCode) {
        jdbcTemplate.update(
                "insert into user_relationship_mode (user_id, relationship_mode_id, created_at) "
                        + "values ((select id from \"user\" where uuid = ?), "
                        + "(select id from relationship_mode where code = ?), now())",
                user.uuid(), modeCode);
    }

    private void block(AuthenticatedUser blocker, AuthenticatedUser blocked) {
        jdbcTemplate.update(
                "insert into block (blocker_user_id, blocked_user_id, created_at) "
                        + "values ((select id from \"user\" where uuid = ?), (select id from \"user\" where uuid = ?), now())",
                blocker.uuid(), blocked.uuid());
    }

    // --- Favorites ---

    @Test
    void favoritingRoundTripsAndRejectsSelfAndUnpublishedTargets() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");
        AuthenticatedUser unpublished = principalFor(newUser());

        assertThat(favoriteService.listMine(me, null, null)).isEmpty();
        favoriteService.add(me, other.uuid());
        assertThat(favoriteService.listMine(me, null, null)).extracting(UserSummaryResponse::id)
                .containsExactly(other.uuid());
        // Adding twice must not fail or duplicate.
        favoriteService.add(me, other.uuid());
        assertThat(favoriteService.listMine(me, null, null)).hasSize(1);

        favoriteService.remove(me, other.uuid());
        assertThat(favoriteService.listMine(me, null, null)).isEmpty();

        assertThatThrownBy(() -> favoriteService.add(me, me.uuid()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_INTERACT_WITH_SELF));
        assertThatThrownBy(() -> favoriteService.add(me, unpublished.uuid()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // --- Discovery ---

    @Test
    void browseExcludesSelfUnpublishedAndWrongModeAndAppliesAgeFilter() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser matchingMode = discoverableUser("MALE", 32, "DATING");
        AuthenticatedUser wrongMode = discoverableUser("MALE", 31, "FRIENDSHIP");
        AuthenticatedUser tooOld = discoverableUser("MALE", 60, "DATING");
        principalFor(newUser()); // unpublished, no profile core at all - must never appear

        List<UserSummaryResponse> results = discoveryService.browse(
                me, "DATING", null, null, null, null, null, null, null, null, null, 0, 50);

        assertThat(results).extracting(UserSummaryResponse::id)
                // tooOld is a legitimate DATING result here: this first call applies no age filter at all.
                .contains(matchingMode.uuid(), tooOld.uuid())
                .doesNotContain(me.uuid(), wrongMode.uuid());

        List<UserSummaryResponse> ageFiltered = discoveryService.browse(
                me, "DATING", 25, 35, null, null, null, null, null, null, null, 0, 50);
        assertThat(ageFiltered).extracting(UserSummaryResponse::id).doesNotContain(tooOld.uuid());
    }

    @Test
    void browseExcludesUsersInEitherBlockDirection() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser blockedByMe = discoverableUser("MALE", 30, "DATING");
        AuthenticatedUser blockedMe = discoverableUser("MALE", 31, "DATING");
        block(me, blockedByMe);
        block(blockedMe, me);

        List<UserSummaryResponse> results = discoveryService.browse(
                me, "DATING", null, null, null, null, null, null, null, null, null, 0, 50);

        assertThat(results).extracting(UserSummaryResponse::id)
                .doesNotContain(blockedByMe.uuid(), blockedMe.uuid());
    }

    @Test
    void recordViewThenAppearsInRecentlyViewedButNotBeforeRecording() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");

        assertThat(discoveryService.recentlyViewed(me, null, null)).isEmpty();
        discoveryService.recordView(me, other.uuid());
        assertThat(discoveryService.recentlyViewed(me, null, null)).extracting(UserSummaryResponse::id)
                .containsExactly(other.uuid());
    }

    @Test
    void homeReturnsProfileCompletionAndDiscoverSlice() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        discoverableUser("MALE", 30, "DATING");

        HomeResponse home = discoveryService.home(me);

        assertThat(home.profileCompletionPercent()).isBetween(0, 100);
        assertThat(home.discoverProfiles()).isNotNull();
    }

    // --- Connections: interests + matches ---

    @Test
    void acceptingAnInterestCreatesAMatchVisibleToBothSides() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");

        InterestResponse sent = interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", "Hi!"));
        assertThat(sent.status()).isEqualTo("PENDING");
        assertThat(interestService.listSent(sender, null, null)).extracting(InterestResponse::id).containsExactly(sent.id());
        assertThat(interestService.listReceived(receiver, null, null)).extracting(InterestResponse::id).containsExactly(sent.id());
        assertThat(matchService.listActive(sender, null, null)).isEmpty();

        InterestResponse accepted = interestService.accept(receiver, sent.id());
        assertThat(accepted.status()).isEqualTo("ACCEPTED");

        List<MatchResponse> senderMatches = matchService.listActive(sender, null, null);
        List<MatchResponse> receiverMatches = matchService.listActive(receiver, null, null);
        assertThat(senderMatches).extracting(m -> m.user().id()).containsExactly(receiver.uuid());
        assertThat(receiverMatches).extracting(m -> m.user().id()).containsExactly(sender.uuid());
        assertThat(senderMatches.get(0).id()).isEqualTo(receiverMatches.get(0).id());
    }

    @Test
    void decliningAnInterestCreatesNoMatch() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null));

        InterestResponse declined = interestService.decline(receiver, sent.id());

        assertThat(declined.status()).isEqualTo("DECLINED");
        assertThat(matchService.listActive(sender, null, null)).isEmpty();
        assertThat(matchService.listActive(receiver, null, null)).isEmpty();
    }

    @Test
    void withdrawingIsSenderOnlyAndOnlyWhilePending() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null));

        assertThatThrownBy(() -> interestService.withdraw(receiver, sent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        InterestResponse withdrawn = interestService.withdraw(sender, sent.id());
        assertThat(withdrawn.status()).isEqualTo("WITHDRAWN");

        assertThatThrownBy(() -> interestService.withdraw(sender, sent.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTEREST_NOT_ACTIONABLE));
    }

    @Test
    void onlyTheReceiverCanAcceptOrDeclineAnInterest() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null));

        assertThatThrownBy(() -> interestService.accept(sender, sent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> interestService.decline(sender, sent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void aNonParticipantCannotSeeOrActOnAnInterestOrMatch() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");
        AuthenticatedUser stranger = discoverableUser("MALE", 29, "DATING");
        InterestResponse sent = interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null));
        interestService.accept(receiver, sent.id());
        MatchResponse match = matchService.getOne(receiver, lookupMatchId(sender));

        assertThatThrownBy(() -> interestService.getOne(stranger, sent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThatThrownBy(() -> interestService.accept(stranger, sent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThatThrownBy(() -> matchService.getOne(stranger, match.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MATCH_NOT_FOUND));
        assertThatThrownBy(() -> matchService.unmatch(stranger, match.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MATCH_NOT_FOUND));
    }

    private UUID lookupMatchId(AuthenticatedUser user) {
        return matchService.listActive(user, null, null).get(0).id();
    }

    @Test
    void sendingASecondPendingInterestForTheSameModeIsRejected() {
        AuthenticatedUser sender = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser receiver = discoverableUser("MALE", 30, "DATING");
        interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null));

        assertThatThrownBy(() -> interestService.send(sender, new CreateInterestRequest(receiver.uuid(), "DATING", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_INTERESTED));
    }

    @Test
    void cannotSendAnInterestToSelfOrToABlockedUser() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser blocked = discoverableUser("MALE", 30, "DATING");
        block(me, blocked);

        assertThatThrownBy(() -> interestService.send(me, new CreateInterestRequest(me.uuid(), "DATING", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_INTERACT_WITH_SELF));
        assertThatThrownBy(() -> interestService.send(me, new CreateInterestRequest(blocked.uuid(), "DATING", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_BLOCKED));
    }

    @Test
    void unmatchMovesAMatchFromActiveToHistoryAndReMatchingRevivesIt() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse first = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, first.id());
        UUID matchId = lookupMatchId(a);

        matchService.unmatch(a, matchId);

        assertThat(matchService.listActive(a, null, null)).isEmpty();
        assertThat(matchService.listHistory(a, null, null)).extracting(MatchResponse::id).containsExactly(matchId);

        InterestResponse second = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, second.id());

        assertThat(matchService.listActive(a, null, null)).extracting(MatchResponse::id).containsExactly(matchId);
        assertThat(matchService.listHistory(a, null, null)).isEmpty();
    }

    @Test
    void compatibilityReturnsANeutralScoreWhenNeitherSideHasSetPreferences() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID matchId = lookupMatchId(a);

        var compatibility = matchService.compatibility(a, matchId);

        assertThat(compatibility.matchId()).isEqualTo(matchId);
        assertThat(compatibility.score()).isEqualTo(50);
        assertThat(compatibility.factors()).isEmpty();
    }

    // --- Messaging: a conversation only ever comes to exist via the match-event listeners in ConversationService,
    // since sending a message is WebSocket-only and not yet implemented - so these insert message rows directly. ---

    @Test
    void acceptingAnInterestAutoCreatesAConversationWithBothParticipants() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());

        List<ConversationResponse> aConversations = conversationService.listMine(a, null, null);
        List<ConversationResponse> bConversations = conversationService.listMine(b, null, null);

        assertThat(aConversations).hasSize(1);
        assertThat(aConversations.get(0).status()).isEqualTo("ACTIVE");
        assertThat(aConversations.get(0).participants()).extracting(UserSummaryResponse::id)
                .containsExactlyInAnyOrder(a.uuid(), b.uuid());
        assertThat(bConversations).extracting(ConversationResponse::id)
                .containsExactly(aConversations.get(0).id());
    }

    @Test
    void unmatchingClosesTheConversationAndRematchingReopensTheSameOne() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse first = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, first.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();
        UUID matchId = lookupMatchId(a);

        matchService.unmatch(a, matchId);
        assertThat(conversationService.getOne(a, conversationId).status()).isEqualTo("CLOSED");

        InterestResponse second = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, second.id());

        ConversationResponse reopened = conversationService.getOne(a, conversationId);
        assertThat(reopened.status()).isEqualTo("ACTIVE");
        assertThat(conversationService.listMine(a, null, null)).extracting(ConversationResponse::id)
                .containsExactly(conversationId);
    }

    @Test
    void nonParticipantCannotAccessAConversationOrItsMessages() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        AuthenticatedUser stranger = discoverableUser("MALE", 29, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();
        UUID messageId = insertMessage(conversationId, a, "Hello!");

        assertThatThrownBy(() -> conversationService.getOne(stranger, conversationId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
        assertThatThrownBy(() -> messageService.getOne(stranger, conversationId, messageId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
        assertThatThrownBy(() -> messageService.history(stranger, conversationId, null, null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    @Test
    void messageHistoryReadAndReactionWorkAndDeleteIsSenderOnly() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();
        UUID messageId = insertMessage(conversationId, a, "Hello Bob!");

        List<MessageResponse> history = messageService.history(b, conversationId, null, null, null);
        assertThat(history).extracting(MessageResponse::id).containsExactly(messageId);
        assertThat(history.get(0).content()).isEqualTo("Hello Bob!");
        assertThat(history.get(0).senderUserId()).isEqualTo(a.uuid());

        messageService.markRead(b, conversationId, messageId);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from message_read mr join \"user\" u on u.id = mr.user_id "
                        + "where mr.read_at is not null and u.uuid = ?", Integer.class, b.uuid()))
                .isEqualTo(1);

        messageService.react(b, conversationId, messageId, new ReactionRequest("heart"));
        assertThat(jdbcTemplate.queryForObject(
                "select mr.reaction_code from message_reaction mr join message m on m.id = mr.message_id "
                        + "where m.uuid = ?", String.class, messageId))
                .isEqualTo("HEART");

        assertThatThrownBy(() -> messageService.delete(b, conversationId, messageId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        messageService.delete(a, conversationId, messageId);
        MessageResponse deleted = messageService.getOne(a, conversationId, messageId);
        assertThat(deleted.status()).isEqualTo("DELETED");
        assertThat(deleted.content()).isNull();
    }

    @Test
    void blockingFromChatClosesTheConversationAndBlocksTheOtherUser() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();

        conversationService.blockParticipant(a, conversationId);

        assertThat(conversationService.getOne(a, conversationId).status()).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from block bl join \"user\" blocker on blocker.id = bl.blocker_user_id "
                        + "join \"user\" blocked on blocked.id = bl.blocked_user_id "
                        + "where blocker.uuid = ? and blocked.uuid = ?", Integer.class, a.uuid(), b.uuid()))
                .isEqualTo(1);
        // Already blocked by discovery's own check (tested separately) - sending a fresh interest is refused too.
        assertThatThrownBy(() -> interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_BLOCKED));
    }

    private UUID insertMessage(UUID conversationId, AuthenticatedUser sender, String content) {
        UUID messageUuid = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into message (uuid, conversation_id, sender_user_id, message_type, content, status, sent_at) "
                        + "values (?, (select id from conversation where uuid = ?), "
                        + "(select id from \"user\" where uuid = ?), 'TEXT', ?, 'SENT', now())",
                messageUuid, conversationId, sender.uuid(), content);
        return messageUuid;
    }

    // --- Trust & Safety ---

    @Test
    void blockingRoundTripsAndIsSelfAndIdempotencySafe() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");

        assertThat(blockService.listMine(me, null, null)).isEmpty();
        blockService.block(me, other.uuid());
        blockService.block(me, other.uuid()); // must not fail or duplicate
        assertThat(blockService.listMine(me, null, null)).extracting(UserSummaryResponse::id).containsExactly(other.uuid());

        blockService.unblock(me, other.uuid());
        assertThat(blockService.listMine(me, null, null)).isEmpty();

        assertThatThrownBy(() -> blockService.block(me, me.uuid()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_INTERACT_WITH_SELF));
    }

    @Test
    void reportingAUserWorksAndRejectsSelfAndUnknownReason() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");
        Long reasonId = reportReasonId("SPAM");

        assertThat(reportService.listReasons()).extracting(r -> r.code()).contains("SPAM", "HARASSMENT");

        ReportResponse report = reportService.create(me, new CreateReportRequest(other.uuid(), null, reasonId, "Kept messaging after I said no"));
        assertThat(report.status()).isEqualTo("OPEN");
        assertThat(report.reasonCode()).isEqualTo("SPAM");
        assertThat(report.reportedUserId()).isEqualTo(other.uuid());

        assertThatThrownBy(() -> reportService.create(me, new CreateReportRequest(me.uuid(), null, reasonId, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_INTERACT_WITH_SELF));
        assertThatThrownBy(() -> reportService.create(me, new CreateReportRequest(other.uuid(), null, 999999L, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void reportingFromAConversationReportsTheOtherParticipant() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();

        ReportResponse report = conversationService.reportParticipant(a, conversationId, reportReasonId("HARASSMENT"), "Threatening messages");

        assertThat(report.reportedUserId()).isEqualTo(b.uuid());
        assertThat(report.reasonCode()).isEqualTo("HARASSMENT");
    }

    @Test
    void reportingAMessageResolvesItsInternalIdThroughMessageLookup() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        interestService.accept(b, sent.id());
        UUID conversationId = conversationService.listMine(a, null, null).get(0).id();
        UUID messageId = insertMessage(conversationId, b, "unwanted message");

        ReportResponse report = reportService.create(a, new CreateReportRequest(null, messageId, reportReasonId("OTHER"), null));

        assertThat(report.reportedMessageId()).isEqualTo(messageId);

        UUID unknownMessage = UUID.randomUUID();
        assertThatThrownBy(() -> reportService.create(a, new CreateReportRequest(null, unknownMessage, reportReasonId("OTHER"), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MESSAGE_NOT_FOUND));
    }

    @Test
    void verificationStartsUnverifiedThenMovesToPendingOnRequestAndIsSelfOnly() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");

        assertThat(verificationService.myStatus(me).status()).isEqualTo("UNVERIFIED");

        VerificationStatusResponse submitted = verificationService.submit(me, new SubmitVerificationRequest("SELFIE", List.of()));
        assertThat(submitted.id()).isNotNull();
        assertThat(submitted.status()).isEqualTo("PENDING");
        assertThat(submitted.verificationType()).isEqualTo("SELFIE");
        assertThat(verificationService.myStatus(me).status()).isEqualTo("PENDING");

        VerificationStatusResponse fetched = verificationService.getOne(me, submitted.id());
        assertThat(fetched.status()).isEqualTo("PENDING");

        assertThatThrownBy(() -> verificationService.getOne(other, submitted.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private Long reportReasonId(String code) {
        return jdbcTemplate.queryForObject("select id from report_reason where code = ?", Long.class, code);
    }

    // --- Support ---

    @Test
    void supportTicketRoundTripsAndIsOwnerOnlyAndLocksOnceResolved() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");

        assertThat(supportTicketService.listMine(me, null, null)).isEmpty();
        SupportTicketResponse created = supportTicketService.create(
                me, new CreateSupportTicketRequest("BILLING", "Can't update payment method", "Details here", "HIGH"));
        assertThat(created.status()).isEqualTo("OPEN");
        assertThat(created.priority()).isEqualTo("HIGH");
        assertThat(supportTicketService.listMine(me, null, null)).extracting(SupportTicketResponse::id).containsExactly(created.id());

        SupportTicketResponse updated = supportTicketService.update(me, created.id(), new UpdateSupportTicketRequest("More detail added"));
        assertThat(updated.description()).isEqualTo("More detail added");

        assertThatThrownBy(() -> supportTicketService.getOne(other, created.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SUPPORT_TICKET_NOT_FOUND));
        assertThatThrownBy(() -> supportTicketService.update(other, created.id(), new UpdateSupportTicketRequest("hijack")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SUPPORT_TICKET_NOT_FOUND));

        jdbcTemplate.update("update support_ticket set status = 'RESOLVED' where uuid = ?", created.id());
        assertThatThrownBy(() -> supportTicketService.update(me, created.id(), new UpdateSupportTicketRequest("too late")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void helpAndLegalContentAreServedAndRejectAnUnknownDocumentType() {
        assertThat(helpContentService.help().topics()).isNotEmpty();
        assertThat(helpContentService.legalDocument("terms").documentType()).isEqualTo("TERMS");

        assertThatThrownBy(() -> helpContentService.legalDocument("NOT_A_REAL_DOC"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // --- Admin ---
    // Role enforcement itself (@PreAuthorize on the admin controllers) isn't exercised here - these tests call the
    // admin services directly, same as every other module in this file, and the role gate is covered separately
    // by the HTTP smoke test. What's under test here is the admin business logic: status transitions, the
    // report -> moderation link, and that every write lands in audit_log.

    @Test
    void adminCanSearchSuspendActivateAndDeleteAUser() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser target = discoverableUser("FEMALE", 27, "MARRIAGE");

        PagedData<AdminUserResponse> found = adminUserService.search(null, target.uuid().toString().substring(0, 8), null, null);
        assertThat(found.items()).extracting(AdminUserResponse::id).contains(target.uuid());

        AdminUserResponse suspended = adminUserService.suspend(admin, target.uuid(), new SuspendUserRequest("Repeated harassment reports"));
        assertThat(suspended.accountStatus()).isEqualTo("SUSPENDED");

        AdminUserResponse activated = adminUserService.activate(admin, target.uuid());
        assertThat(activated.accountStatus()).isEqualTo("ACTIVE");

        adminUserService.delete(admin, target.uuid());
        assertThat(adminUserService.getOne(target.uuid()).accountStatus()).isEqualTo("DELETED");

        assertThatThrownBy(() -> adminUserService.suspend(admin, target.uuid(), new SuspendUserRequest("too late")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        PagedData<AuditLogResponse> log = auditLogService.list(null, null);
        assertThat(log.items()).extracting(AuditLogResponse::action)
                .contains("USER_SUSPENDED", "USER_ACTIVATED", "USER_DELETED");
    }

    @Test
    void adminCanReassignUserRolesAndRejectsUnknownCodes() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser target = userWithProfile();

        AdminUserResponse updated = adminUserService.updateRoles(admin, target.uuid(), new UpdateUserRolesRequest(List.of("MODERATOR")));
        assertThat(updated.roles()).containsExactly("MODERATOR");

        assertThatThrownBy(() -> adminUserService.updateRoles(admin, target.uuid(), new UpdateUserRolesRequest(List.of("NOT_A_ROLE"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void adminLoginHistoryListsThatUsersEntriesOnly() {
        AuthenticatedUser target = userWithProfile();
        AuthenticatedUser other = userWithProfile();
        insertLoginHistory(target, true, null);
        insertLoginHistory(target, false, "BAD_PASSWORD");
        insertLoginHistory(other, true, null);

        PagedData<LoginHistoryResponse> history = adminUserService.loginHistory(target.uuid(), null, null);

        assertThat(history.items()).hasSize(2);
        assertThat(history.items()).extracting(LoginHistoryResponse::success).contains(true, false);
    }

    @Test
    void adminApproveAndRejectMoveAVerificationOutOfPending() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser other = discoverableUser("MALE", 30, "DATING");
        VerificationStatusResponse mine = verificationService.submit(me, new SubmitVerificationRequest("SELFIE", List.of()));
        VerificationStatusResponse theirs = verificationService.submit(other, new SubmitVerificationRequest("ID_CARD", List.of()));

        PagedData<AdminVerificationResponse> pending = adminVerificationService.listPending(null, null);
        assertThat(pending.items()).extracting(AdminVerificationResponse::id).contains(mine.id(), theirs.id());

        AdminVerificationResponse approved = adminVerificationService.approve(admin, mine.id(), new VerificationDecisionRequest("Looks good"));
        assertThat(approved.verificationStatus()).isEqualTo("VERIFIED");
        assertThat(verificationService.myStatus(me).status()).isEqualTo("VERIFIED");

        AdminVerificationResponse rejected = adminVerificationService.reject(admin, theirs.id(), new VerificationDecisionRequest("Blurry photo"));
        assertThat(rejected.verificationStatus()).isEqualTo("REJECTED");

        assertThatThrownBy(() -> adminVerificationService.approve(admin, mine.id(), null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    @Test
    void resolvingAReportWithAModerationActionOpensAModerationEntry() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser reporter = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser reported = discoverableUser("MALE", 30, "DATING");
        ReportResponse report = reportService.create(
                reporter, new CreateReportRequest(reported.uuid(), null, reportReasonId("HARASSMENT"), "Won't stop messaging"));

        assertThat(adminReportService.list(null, null, null).items()).extracting(AdminReportResponse::id).contains(report.id());
        assertThat(adminReportService.getOne(report.id()).status()).isEqualTo("OPEN");

        AdminReportResponse resolved = adminReportService.resolve(
                admin, report.id(), new ResolveReportRequest("RESOLVED", "SUSPEND", "Confirmed harassment"));
        assertThat(resolved.status()).isEqualTo("RESOLVED");

        PagedData<AdminModerationResponse> moderation = adminModerationService.list(null, null, null);
        assertThat(moderation.items()).anySatisfy(entry -> {
            assertThat(entry.targetUserId()).isEqualTo(reported.uuid());
            assertThat(entry.action()).isEqualTo("SUSPEND");
            assertThat(entry.status()).isEqualTo("OPEN");
        });

        assertThatThrownBy(() -> adminReportService.resolve(admin, report.id(), new ResolveReportRequest("RESOLVED", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void adminCanOpenAndResolveAStandaloneModerationAction() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser target = userWithProfile();

        AdminModerationResponse opened = adminModerationService.create(
                admin, new CreateModerationRequest(target.uuid(), "WARN", "Inappropriate profile photo"));
        assertThat(opened.status()).isEqualTo("OPEN");
        assertThat(opened.action()).isEqualTo("WARN");

        AdminModerationResponse resolved = adminModerationService.resolve(admin, opened.id(), new ResolveModerationRequest("Photo removed by user"));
        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.resolvedAt()).isNotNull();

        assertThatThrownBy(() -> adminModerationService.resolve(admin, opened.id(), null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void adminSupportTicketAssignDefaultsToCallerAndResolveClosesIt() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser me = userWithProfile();
        SupportTicketResponse ticket = supportTicketService.create(
                me, new CreateSupportTicketRequest("ACCOUNT", "Can't log in", "Locked out after too many attempts", "URGENT"));

        assertThat(adminSupportService.list(null, null, null).items()).extracting(AdminSupportTicketResponse::id).contains(ticket.id());

        AdminSupportTicketResponse assigned = adminSupportService.assign(admin, ticket.id(), new AssignTicketRequest(null));
        assertThat(assigned.assignedToId()).isEqualTo(admin.uuid());
        assertThat(assigned.status()).isEqualTo("IN_PROGRESS");

        AdminSupportTicketResponse resolved = adminSupportService.resolve(admin, ticket.id(), new ResolveTicketRequest("Password reset for the user"));
        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.resolvedAt()).isNotNull();
    }

    @Test
    void adminSettingsUpsertCreatesThenUpdatesTheSameKey() {
        AuthenticatedUser admin = principalFor(newUser());
        String key = "discovery.daily_like_limit." + UUID.randomUUID();

        AdminSettingResponse created = adminSettingService.upsert(admin, key, new UpdateSettingRequest("50"));
        assertThat(created.value()).isEqualTo("50");
        assertThat(adminSettingService.list()).extracting(AdminSettingResponse::key).contains(key);

        AdminSettingResponse updated = adminSettingService.upsert(admin, key, new UpdateSettingRequest("75"));
        assertThat(updated.value()).isEqualTo("75");
        assertThat(adminSettingService.list().stream().filter(s -> s.key().equals(key)).count()).isEqualTo(1);
    }

    private void insertLoginHistory(AuthenticatedUser user, boolean success, String failureReason) {
        jdbcTemplate.update(
                "insert into login_history (user_id, login_type, success, failure_reason, login_at) "
                        + "values ((select id from \"user\" where uuid = ?), 'PASSWORD', ?, ?, now())",
                user.uuid(), success, failureReason);
    }

    // --- Notifications ---
    // Each producer module (connections, admin) publishes a domain event with zero knowledge that notifications
    // listens; these tests drive the producing action through its own service and assert the resulting
    // notification, exercising the whole event -> listener -> persisted row chain rather than the listener alone.

    @Test
    void sendingAnInterestNotifiesTheReceiverAndCanBeMarkedRead() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");

        interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));

        List<NotificationResponse> unread = notificationService.list(b, "INTEREST_RECEIVED", true, null, null);
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).isRead()).isFalse();
        assertThat(notificationService.list(a, "INTEREST_RECEIVED", null, null, null)).isEmpty();

        NotificationResponse marked = notificationService.markRead(b, unread.get(0).id());
        assertThat(marked.isRead()).isTrue();
        assertThat(marked.readAt()).isNotNull();
        assertThat(notificationService.list(b, "INTEREST_RECEIVED", true, null, null)).isEmpty();
    }

    @Test
    void acceptingAnInterestNotifiesBothMatchedUsers() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));

        interestService.accept(b, sent.id());

        assertThat(notificationService.list(a, "MATCH_CREATED", null, null, null)).hasSize(1);
        assertThat(notificationService.list(b, "MATCH_CREATED", null, null, null)).hasSize(1);
    }

    @Test
    void adminVerificationDecisionsNotifyTheProfileOwner() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser approved = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser rejected = discoverableUser("MALE", 30, "DATING");
        VerificationStatusResponse approvedSubmission = verificationService.submit(approved, new SubmitVerificationRequest("SELFIE", List.of()));
        VerificationStatusResponse rejectedSubmission = verificationService.submit(rejected, new SubmitVerificationRequest("ID_CARD", List.of()));

        adminVerificationService.approve(admin, approvedSubmission.id(), null);
        adminVerificationService.reject(admin, rejectedSubmission.id(), null);

        assertThat(notificationService.list(approved, "VERIFICATION_APPROVED", null, null, null)).hasSize(1);
        assertThat(notificationService.list(rejected, "VERIFICATION_REJECTED", null, null, null)).hasSize(1);
    }

    @Test
    void resolvingAReportNotifiesTheReporter() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser reporter = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser reported = discoverableUser("MALE", 30, "DATING");
        ReportResponse report = reportService.create(
                reporter, new CreateReportRequest(reported.uuid(), null, reportReasonId("SPAM"), null));

        adminReportService.resolve(admin, report.id(), new ResolveReportRequest("DISMISSED", null, null));

        assertThat(notificationService.list(reporter, "REPORT_RESOLVED", null, null, null)).hasSize(1);
        assertThat(notificationService.list(reported, "REPORT_RESOLVED", null, null, null)).isEmpty();
    }

    @Test
    void suspendingAndReactivatingAUserNotifiesThem() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser target = discoverableUser("FEMALE", 27, "MARRIAGE");

        adminUserService.suspend(admin, target.uuid(), new SuspendUserRequest("Policy violation"));
        // Not notificationService.list(target, ...) here: AccountStateGuard.assertUsableSession() blocks every
        // self-service call, including this one, while SUSPENDED - by design, same as every other endpoint. The
        // notification row still exists; check it directly, the way a moderator/admin path would have to.
        assertThat(countNotifications(target, "ACCOUNT_SUSPENDED")).isEqualTo(1);

        adminUserService.activate(admin, target.uuid());
        assertThat(notificationService.list(target, "ACCOUNT_REACTIVATED", null, null, null)).hasSize(1);
        assertThat(notificationService.list(target, "ACCOUNT_SUSPENDED", null, null, null)).hasSize(1);
    }

    private long countNotifications(AuthenticatedUser user, String type) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notification where type = ? and user_id = (select id from \"user\" where uuid = ?)",
                Long.class, type, user.uuid());
    }

    @Test
    void resolvingASupportTicketNotifiesItsOwner() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser owner = userWithProfile();
        SupportTicketResponse ticket = supportTicketService.create(
                owner, new CreateSupportTicketRequest("ACCOUNT", "Can't log in", "Locked out", "URGENT"));

        adminSupportService.resolve(admin, ticket.id(), null);

        assertThat(notificationService.list(owner, "SUPPORT_TICKET_RESOLVED", null, null, null)).hasSize(1);
    }

    @Test
    void notificationSettingsDefaultToEnabledAndOptOutSuppressesFutureNotifications() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");

        List<NotificationSettingResponse> defaults = notificationSettingService.get(b);
        assertThat(defaults).hasSize(NotificationType.values().length);
        assertThat(defaults).allSatisfy(s -> {
            assertThat(s.inAppEnabled()).isTrue();
            assertThat(s.smsEnabled()).isFalse();
        });

        List<NotificationSettingResponse> updated = notificationSettingService.update(
                b, new UpdateNotificationSettingsRequest(List.of(
                        new NotificationSettingUpdate("INTEREST_RECEIVED", false, null, null, null))));
        assertThat(updated).filteredOn(s -> s.notificationType().equals("INTEREST_RECEIVED"))
                .extracting(NotificationSettingResponse::inAppEnabled).containsExactly(false);

        interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        assertThat(notificationService.list(b, "INTEREST_RECEIVED", null, null, null)).isEmpty();
    }

    @Test
    void deleteAndMarkAllReadAreOwnerOnly() {
        AuthenticatedUser a = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser b = discoverableUser("MALE", 30, "DATING");
        interestService.send(a, new CreateInterestRequest(b.uuid(), "DATING", null));
        NotificationResponse notification = notificationService.list(b, null, null, null, null).get(0);

        assertThatThrownBy(() -> notificationService.getOne(a, notification.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
        assertThatThrownBy(() -> notificationService.delete(a, notification.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));

        notificationService.markAllRead(b);
        assertThat(notificationService.list(b, null, true, null, null)).isEmpty();

        notificationService.delete(b, notification.id());
        assertThatThrownBy(() -> notificationService.getOne(b, notification.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    // --- Discovery add-ons: recommendations, saved searches, search history ---

    @Test
    void refreshingRecommendationsScoresAndPersistsCandidatesThenListReturnsThem() {
        AuthenticatedUser self = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser candidateOne = discoverableUser("MALE", 30, "DATING");
        AuthenticatedUser candidateTwo = discoverableUser("MALE", 33, "DATING");

        assertThat(recommendationService.list(self, null, null)).isEmpty();

        int generated = recommendationService.refresh(self);

        assertThat(generated).isGreaterThanOrEqualTo(2);
        List<UserSummaryResponse> recommended = recommendationService.list(self, null, null);
        assertThat(recommended).extracting(UserSummaryResponse::id).contains(candidateOne.uuid(), candidateTwo.uuid());
        assertThat(recommended).extracting(UserSummaryResponse::id).doesNotContain(self.uuid());

        // Refreshing again replaces the prior snapshot rather than accumulating duplicates.
        int regenerated = recommendationService.refresh(self);
        assertThat(regenerated).isEqualTo(generated);
        assertThat(countRecommendations(self)).isEqualTo(generated);
    }

    private long countRecommendations(AuthenticatedUser user) {
        return jdbcTemplate.queryForObject(
                "select count(*) from recommendation where user_id = (select id from \"user\" where uuid = ?)",
                Long.class, user.uuid());
    }

    @Test
    void savedSearchCrudRoundTripsAndEnforcesASingleDefaultAndOwnership() {
        AuthenticatedUser me = userWithProfile();
        AuthenticatedUser other = userWithProfile();

        SavedSearchResponse first = savedSearchService.create(
                me, new CreateSavedSearchRequest("Nearby matches", Map.of("cityId", 1), true));
        assertThat(first.isDefault()).isTrue();
        assertThat(first.criteria()).containsEntry("cityId", 1);

        SavedSearchResponse second = savedSearchService.create(
                me, new CreateSavedSearchRequest("Spiritual seekers", Map.of("spiritualCommunity", "Vedanta"), true));
        assertThat(second.isDefault()).isTrue();

        List<SavedSearchResponse> mine = savedSearchService.list(me);
        assertThat(mine).hasSize(2);
        assertThat(mine.stream().filter(SavedSearchResponse::isDefault)).hasSize(1);

        SavedSearchResponse updated = savedSearchService.update(
                me, first.id(), new UpdateSavedSearchRequest("Renamed search", null, null));
        assertThat(updated.name()).isEqualTo("Renamed search");
        assertThat(updated.criteria()).containsEntry("cityId", 1);

        assertThatThrownBy(() -> savedSearchService.update(other, first.id(), new UpdateSavedSearchRequest("hijack", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SAVED_SEARCH_NOT_FOUND));
        assertThatThrownBy(() -> savedSearchService.delete(other, first.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SAVED_SEARCH_NOT_FOUND));

        savedSearchService.delete(me, first.id());
        assertThat(savedSearchService.list(me)).extracting(SavedSearchResponse::id).containsExactly(second.id());
    }

    @Test
    void searchingRecordsHistoryAndClearRemovesIt() {
        AuthenticatedUser me = discoverableUser("FEMALE", 28, "DATING");
        discoverableUser("MALE", 30, "DATING");

        discoveryService.search(me, new DiscoverySearchRequest(null, List.of("DATING"), 25, 40, null, null, null, null, null, null));

        List<SearchHistoryResponse> history = searchHistoryService.list(me, null, null);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).criteria()).containsEntry("minAge", 25);

        searchHistoryService.clear(me);
        assertThat(searchHistoryService.list(me, null, null)).isEmpty();
    }

    // --- Appeals, and discovery collections ---

    @Test
    void submittingAnAppealAndAdminApprovingItNotifiesTheAppellant() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser me = userWithProfile();

        AppealResponse submitted = appealService.submit(me, new SubmitAppealRequest("ACCOUNT_SUSPENSION", "I was suspended by mistake"));
        assertThat(submitted.status()).isEqualTo("OPEN");

        assertThat(adminAppealService.list(null, null, null).items()).extracting(AdminAppealResponse::id).contains(submitted.id());
        assertThat(adminAppealService.getOne(submitted.id()).status()).isEqualTo("OPEN");

        AdminAppealResponse resolved = adminAppealService.resolve(
                admin, submitted.id(), new ResolveAppealRequest("APPROVED", "Confirmed - reinstating the account"));
        assertThat(resolved.status()).isEqualTo("APPROVED");

        assertThat(notificationService.list(me, "APPEAL_RESOLVED", null, null, null)).hasSize(1);

        assertThatThrownBy(() -> adminAppealService.resolve(admin, submitted.id(), new ResolveAppealRequest("REJECTED", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void appealResolutionRejectsAnUnknownOrOpenStatusValue() {
        AuthenticatedUser admin = principalFor(newUser());
        AuthenticatedUser me = userWithProfile();
        AppealResponse submitted = appealService.submit(me, new SubmitAppealRequest("OTHER", "Details"));

        assertThatThrownBy(() -> adminAppealService.resolve(admin, submitted.id(), new ResolveAppealRequest("OPEN", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThatThrownBy(() -> adminAppealService.resolve(admin, submitted.id(), new ResolveAppealRequest("NOT_A_STATUS", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void discoveryCollectionsAreSeededAndDetailComputesMembersLiveFromCriteria() {
        AuthenticatedUser viewer = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser candidate = discoverableUser("MALE", 30, "DATING");

        List<CollectionSummaryResponse> collections = discoveryCollectionService.list();
        assertThat(collections).extracting(CollectionSummaryResponse::code).contains("new-members");

        CollectionDetailResponse detail = discoveryCollectionService.getOne(viewer, "new-members", null, null);
        assertThat(detail.code()).isEqualTo("new-members");
        assertThat(detail.members()).extracting(UserSummaryResponse::id).contains(candidate.uuid());
        assertThat(detail.members()).extracting(UserSummaryResponse::id).doesNotContain(viewer.uuid());

        assertThatThrownBy(() -> discoveryCollectionService.getOne(viewer, "not-a-real-collection", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COLLECTION_NOT_FOUND));
    }

    // --- Subscriptions, payments, invoices, coupons ---
    // The gateway itself (AutoApprovePaymentGateway) is a stub that always succeeds - see its own class comment
    // for why. What's under test here is everything around it: the subscription lifecycle, payment/invoice
    // bookkeeping, coupon discount/redemption, and that a FAILED payment (only reachable here by flipping it
    // via SQL, since the stub never produces one organically) can actually be retried.

    @Test
    void plansAreSeededAndPubliclyListed() {
        List<PlanResponse> plans = planService.list();
        assertThat(plans).extracting(PlanResponse::code).contains("PREMIUM_MONTHLY", "PREMIUM_QUARTERLY", "PREMIUM_YEARLY");

        PlanResponse monthly = planService.getOne("PREMIUM_MONTHLY");
        assertThat(monthly.billingPeriod()).isEqualTo("MONTHLY");
        assertThat(monthly.durationDays()).isEqualTo(30);
        assertThat(monthly.features()).containsKey("unlimitedInterests");

        assertThatThrownBy(() -> planService.getOne("NOT_A_PLAN"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAN_NOT_FOUND));
    }

    @Test
    void startingASubscriptionChargesInvoicesAndActivatesViaTheStubGatewayAndBlocksASecondCheckout() {
        AuthenticatedUser me = principalFor(newUser());
        assertThat(subscriptionService.getCurrent(me)).isNull();

        SubscriptionResponse started = subscriptionService.start(me, new StartSubscriptionRequest("PREMIUM_MONTHLY", null, null));
        assertThat(started.status()).isEqualTo("ACTIVE");
        assertThat(started.plan().code()).isEqualTo("PREMIUM_MONTHLY");
        assertThat(started.autoRenew()).isFalse();

        assertThat(subscriptionService.getCurrent(me).id()).isEqualTo(started.id());

        List<PaymentResponse> payments = paymentService.listMine(me, null, null);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).status()).isEqualTo("SUCCESS");
        assertThat(payments.get(0).amount()).isEqualByComparingTo(new BigDecimal("999.00"));
        assertThat(payments.get(0).subscriptionId()).isEqualTo(started.id());

        List<InvoiceResponse> invoices = invoiceService.listMine(me, null, null);
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).status()).isEqualTo("PAID");
        assertThat(invoices.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("999.00"));

        assertThat(notificationService.list(me, "PAYMENT_SUCCEEDED", null, null, null)).hasSize(1);

        assertThatThrownBy(() -> subscriptionService.start(me, new StartSubscriptionRequest("PREMIUM_YEARLY", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void startingWithACouponDiscountsThePaymentAndIncrementsRedemptionCount() {
        AuthenticatedUser me = principalFor(newUser());
        int redemptionsBefore = countCouponRedemptions("WELCOME10");

        SubscriptionResponse started = subscriptionService.start(me, new StartSubscriptionRequest("PREMIUM_MONTHLY", "WELCOME10", null));

        assertThat(started.status()).isEqualTo("ACTIVE");
        PaymentResponse payment = paymentService.listMine(me, null, null).get(0);
        assertThat(payment.amount()).isEqualByComparingTo(new BigDecimal("899.10")); // 999.00 - 10%
        assertThat(countCouponRedemptions("WELCOME10")).isEqualTo(redemptionsBefore + 1);
    }

    private int countCouponRedemptions(String code) {
        return jdbcTemplate.queryForObject("select redemption_count from coupon where code = ?", Integer.class, code);
    }

    @Test
    void couponValidateReportsDiscountForAGoodCodeAndAReasonForABadOne() {
        BigDecimal price = new BigDecimal("999.00");

        CouponValidationResponse valid = couponService.validate(new ValidateCouponRequest("WELCOME10", "PREMIUM_MONTHLY"), price);
        assertThat(valid.valid()).isTrue();
        assertThat(valid.discountedPrice()).isEqualByComparingTo(new BigDecimal("899.10"));

        CouponValidationResponse unknown = couponService.validate(new ValidateCouponRequest("NOT_A_CODE", "PREMIUM_MONTHLY"), price);
        assertThat(unknown.valid()).isFalse();
        assertThat(unknown.reason()).isNotBlank();
    }

    @Test
    void cancelStopsAutoRenewAndBlocksFurtherAutoRenewChangesAndDoubleCancellation() {
        AuthenticatedUser me = principalFor(newUser());
        SubscriptionResponse started = subscriptionService.start(me, new StartSubscriptionRequest("PREMIUM_MONTHLY", null, null));

        SubscriptionResponse withAutoRenew = subscriptionService.setAutoRenew(me, started.id(), new UpdateAutoRenewRequest(true));
        assertThat(withAutoRenew.autoRenew()).isTrue();

        SubscriptionResponse cancelled = subscriptionService.cancel(me, started.id());
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.autoRenew()).isFalse();

        assertThatThrownBy(() -> subscriptionService.cancel(me, started.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThatThrownBy(() -> subscriptionService.setAutoRenew(me, started.id(), new UpdateAutoRenewRequest(true)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_ACTIVE));

        AuthenticatedUser other = principalFor(newUser());
        assertThatThrownBy(() -> subscriptionService.getOne(other, started.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * The stub gateway auto-confirms every checkout, so start() never organically leaves a FAILED payment
     * behind - that only happens with a real, asynchronous gateway whose first mandate authorization failed.
     * Builds that state directly (a still-PENDING subscription with a FAILED payment row) to exercise
     * SubscriptionService.retryFailedCheckout()'s actual guards and its confirm-on-success path.
     */
    @Test
    void retryOnlyWorksOnAFailedFirstPaymentOfAStillPendingSubscriptionAndFiresASucceededNotification() {
        AuthenticatedUser me = principalFor(newUser());
        UUID failedPaymentId = insertPendingSubscriptionWithFailedFirstPayment(me, "PREMIUM_MONTHLY");
        assertThat(notificationService.list(me, "PAYMENT_SUCCEEDED", null, null, null)).isEmpty();

        SubscriptionResponse retried = subscriptionService.retryFailedCheckout(me, failedPaymentId);
        assertThat(retried.status()).isEqualTo("ACTIVE");
        assertThat(notificationService.list(me, "PAYMENT_SUCCEEDED", null, null, null)).hasSize(1);

        // The subscription is ACTIVE now, so the same (still-FAILED) payment id can no longer be retried -
        // once a mandate is authorized, a failed renewal is the provider's own retry schedule, not ours.
        assertThatThrownBy(() -> subscriptionService.retryFailedCheckout(me, failedPaymentId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        AuthenticatedUser other = principalFor(newUser());
        assertThatThrownBy(() -> subscriptionService.retryFailedCheckout(other, failedPaymentId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private UUID insertPendingSubscriptionWithFailedFirstPayment(AuthenticatedUser user, String planCode) {
        UUID subscriptionUuid = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into subscription (uuid, user_id, plan_id, status, starts_at, auto_renew, created_at, updated_at) "
                        + "values (?, (select id from \"user\" where uuid = ?), (select id from plan where code = ?), "
                        + "'PENDING', now(), false, now(), now())",
                subscriptionUuid, user.uuid(), planCode);
        UUID paymentUuid = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into payment (uuid, user_id, subscription_id, provider, amount, currency_code, status, created_at) "
                        + "values (?, (select id from \"user\" where uuid = ?), (select id from subscription where uuid = ?), "
                        + "'STUB', 999.00, 'INR', 'FAILED', now())",
                paymentUuid, user.uuid(), subscriptionUuid);
        return paymentUuid;
    }
}
