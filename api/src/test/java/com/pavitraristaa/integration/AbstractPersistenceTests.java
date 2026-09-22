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
}
