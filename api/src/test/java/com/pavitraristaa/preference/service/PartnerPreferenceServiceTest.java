package com.pavitraristaa.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import com.pavitraristaa.preference.dto.PartnerPreferenceRequest;
import com.pavitraristaa.preference.dto.PreferenceValueRequest;
import com.pavitraristaa.preference.repository.PartnerPreferenceRepository;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerPreferenceServiceTest {

    @Mock private AuthService authService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PartnerPreferenceRepository partnerPreferenceRepository;
    @Mock private MasterValueResolver masterValueResolver;

    private PartnerPreferenceService service;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        service = new PartnerPreferenceService(
                authService, userProfileRepository, partnerPreferenceRepository, masterValueResolver,
                new PavitraProperties());
        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setUuid(UUID.randomUUID());
        user.setAccountStatus(AccountStatus.ACTIVE);
        principal = new AuthenticatedUser(user.getId(), user.getUuid(), List.of("USER"), 1L);
        lenient().when(authService.requireUsable(principal)).thenReturn(user);
        lenient().when(userProfileRepository.findByUserAndDeletedFalse(user)).thenReturn(Optional.of(new UserProfile()));
    }

    @Test
    void rejectsMinimumAgeBelowPlatformMinimum() {
        assertRejected(request(17, null, null, null, null, null));
    }

    @Test
    void rejectsInvertedAgeRange() {
        assertRejected(request(40, 30, null, null, null, null));
    }

    @Test
    void rejectsInvertedHeightRange() {
        assertRejected(request(null, null, 190, 170, null, null));
    }

    @Test
    void rejectsInvertedIncomeRange() {
        assertRejected(request(null, null, null, null, new BigDecimal("900"), new BigDecimal("100")));
    }

    @Test
    void requiresCurrencyWhenIncomeIsGiven() {
        assertRejected(request(null, null, null, null, new BigDecimal("100"), null));
    }

    @Test
    void rejectsUnknownPreferenceType() {
        PartnerPreferenceRequest request = new PartnerPreferenceRequest(
                null, null, null, null, null, null, null, null, null, null,
                List.of(new PreferenceValueRequest("COLOR", 1L)));

        assertRejected(request);
    }

    @Test
    void requiresAProfile() {
        UserAccount other = new UserAccount();
        other.setUuid(UUID.randomUUID());
        AuthenticatedUser noProfile = new AuthenticatedUser(8L, other.getUuid(), List.of("USER"), 2L);
        when(authService.requireUsable(noProfile)).thenReturn(other);
        when(userProfileRepository.findByUserAndDeletedFalse(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(noProfile))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void assertRejected(PartnerPreferenceRequest request) {
        assertThatThrownBy(() -> service.replaceMine(principal, request))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(partnerPreferenceRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private PartnerPreferenceRequest request(
            Integer minAge, Integer maxAge, Integer minHeight, Integer maxHeight,
            BigDecimal minIncome, BigDecimal maxIncome
    ) {
        return new PartnerPreferenceRequest(
                minAge, maxAge, minHeight, maxHeight, null, minIncome, maxIncome, null, null, null, null);
    }
}
