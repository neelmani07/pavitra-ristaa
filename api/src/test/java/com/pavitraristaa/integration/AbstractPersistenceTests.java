package com.pavitraristaa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.pavitraristaa.discovery.dto.HomeResponse;
import com.pavitraristaa.discovery.service.DiscoveryService;
import com.pavitraristaa.favorites.service.FavoriteService;
import com.pavitraristaa.master.dto.MasterValueResponse;
import com.pavitraristaa.master.service.MasterDataService;
import com.pavitraristaa.media.dto.CompleteUploadRequest;
import com.pavitraristaa.media.service.MediaService;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
}
