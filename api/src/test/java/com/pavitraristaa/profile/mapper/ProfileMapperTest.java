package com.pavitraristaa.profile.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.media.service.MediaUrlResolver;
import com.pavitraristaa.profile.dto.ProfilePhotoResponse;
import com.pavitraristaa.profile.entity.PhotoApprovalStatus;
import com.pavitraristaa.profile.entity.PhotoType;
import com.pavitraristaa.profile.entity.PhotoVisibility;
import com.pavitraristaa.profile.entity.ProfilePhoto;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.UserProfile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the photo-visibility bug: a photo still awaiting moderator approval, or one a
 * moderator rejected, must never appear on the public profile - only visibility was checked before this fix.
 */
class ProfileMapperTest {

    private final ProfileMapper mapper = new ProfileMapper(mock(MediaUrlResolver.class));

    @Test
    void strangersOnlySeeApprovedNonPrivatePhotos() {
        UserProfile profile = profileWith(
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.APPROVED, "approved-public"),
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.PENDING, "approved-pending"),
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.REJECTED, "approved-rejected"),
                photo(PhotoVisibility.MATCHES, PhotoApprovalStatus.APPROVED, "approved-matches"),
                photo(PhotoVisibility.PRIVATE, PhotoApprovalStatus.APPROVED, "approved-private")
        );

        List<ProfilePhotoResponse> visible = mapper.toResponse(profile, List.of(), false).photos();

        assertThat(visible).hasSize(2);
        assertThat(visible).allSatisfy(p -> {
            assertThat(p.approvalStatus()).isEqualTo("APPROVED");
            assertThat(p.visibility()).isIn("PUBLIC", "MATCHES");
        });
    }

    @Test
    void ownerSeesEveryPhotoRegardlessOfApprovalOrVisibility() {
        UserProfile profile = profileWith(
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.APPROVED, "approved-public"),
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.PENDING, "pending"),
                photo(PhotoVisibility.PUBLIC, PhotoApprovalStatus.REJECTED, "rejected"),
                photo(PhotoVisibility.PRIVATE, PhotoApprovalStatus.APPROVED, "private")
        );

        List<ProfilePhotoResponse> visible = mapper.toResponse(profile, List.of(), true).photos();

        assertThat(visible).hasSize(4);
    }

    private UserProfile profileWith(ProfilePhoto... photos) {
        UserProfile profile = new UserProfile();
        profile.setUser(new UserAccount());
        profile.getUser().setUuid(UUID.randomUUID());
        profile.setFirstName("Asha");
        profile.setDateOfBirth(LocalDate.now().minusYears(28));
        profile.setGender("FEMALE");
        profile.setProfileStatus(ProfileStatus.ACTIVE);
        profile.setProfileCompletionPercent((short) 100);
        for (ProfilePhoto photo : photos) {
            profile.getPhotos().add(photo);
        }
        return profile;
    }

    private ProfilePhoto photo(PhotoVisibility visibility, PhotoApprovalStatus approvalStatus, String debugName) {
        ProfilePhoto photo = new ProfilePhoto();
        photo.setUuid(UUID.randomUUID());
        photo.setPhotoType(PhotoType.PROFILE);
        photo.setDisplayOrder((short) 0);
        photo.setVisibility(visibility);
        photo.setApprovalStatus(approvalStatus);
        return photo;
    }
}
