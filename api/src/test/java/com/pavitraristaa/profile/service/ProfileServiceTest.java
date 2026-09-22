package com.pavitraristaa.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.master.service.MasterValueResolver;
import com.pavitraristaa.media.service.MediaUrlResolver;
import com.pavitraristaa.media.repository.MediaFileRepository;
import com.pavitraristaa.profile.dto.UpdateProfileRequest;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.ProfileMapper;
import com.pavitraristaa.profile.repository.ProfilePhotoRepository;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.relationship.repository.UserRelationshipModeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private AuthService authService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private ProfilePhotoRepository profilePhotoRepository;
    @Mock private UserRelationshipModeRepository userRelationshipModeRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private MasterValueResolver masterValueResolver;
    @Mock private MediaUrlResolver mediaUrlResolver;

    private ProfileService profileService;
    private UserAccount user;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                authService,
                userProfileRepository,
                profilePhotoRepository,
                userRelationshipModeRepository,
                mediaFileRepository,
                masterValueResolver,
                new ProfileMapper(mediaUrlResolver),
                new PavitraProperties()
        );
        user = new UserAccount();
        user.setId(7L);
        user.setUuid(UUID.randomUUID());
        user.setAccountStatus(AccountStatus.ACTIVE);
        principal = new AuthenticatedUser(user.getId(), user.getUuid(), List.of("USER"), 1L);
        when(authService.requireUsable(principal)).thenReturn(user);
    }

    @Test
    void updateCoreCreatesDraftProfile() {
        when(userProfileRepository.findByUserAndDeletedFalse(user)).thenReturn(Optional.empty());
        when(userRelationshipModeRepository.findByUser(user)).thenReturn(List.of());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = profileService.updateCore(principal, new UpdateProfileRequest(
                "Ada",
                "Lovelace",
                "Ada",
                LocalDate.now().minusYears(28),
                "FEMALE",
                "Mathematician",
                "About me",
                null,
                null,
                null
        ));

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.profileStatus()).isEqualTo("DRAFT");
        assertThat(response.id()).isEqualTo(user.getUuid());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getProfileStatus()).isEqualTo(ProfileStatus.DRAFT);
        assertThat(captor.getValue().getProfileCompletionPercent()).isGreaterThan((short) 0);
    }

    @Test
    void updateCoreRejectsUnderageUser() {
        when(userProfileRepository.findByUserAndDeletedFalse(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateCore(principal, new UpdateProfileRequest(
                "Ada",
                null,
                null,
                LocalDate.now().minusYears(16),
                "FEMALE",
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getMineThrowsWhenProfileMissing() {
        when(userProfileRepository.findByUserAndDeletedFalse(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getMine(principal))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
