package com.pavitraristaa.preference.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.master.service.MasterValueResolver;
import com.pavitraristaa.preference.dto.PartnerPreferenceRequest;
import com.pavitraristaa.preference.dto.PartnerPreferenceResponse;
import com.pavitraristaa.preference.dto.PreferenceValueRequest;
import com.pavitraristaa.preference.dto.PreferenceValueResponse;
import com.pavitraristaa.preference.entity.PartnerPreference;
import com.pavitraristaa.preference.entity.PartnerPreferenceValue;
import com.pavitraristaa.preference.entity.PreferenceType;
import com.pavitraristaa.preference.repository.PartnerPreferenceRepository;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single partner-preference resource. It is shared by Dating, Friendship and Marriage; relationship mode is
 * context only and never selects a different preference set.
 */
@Service
public class PartnerPreferenceService {

    private static final String MARITAL_STATUS_CATEGORY = "MARITAL_STATUS";

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final MasterValueResolver masterValueResolver;
    private final PavitraProperties properties;

    public PartnerPreferenceService(
            AuthService authService,
            UserProfileRepository userProfileRepository,
            PartnerPreferenceRepository partnerPreferenceRepository,
            MasterValueResolver masterValueResolver,
            PavitraProperties properties
    ) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.partnerPreferenceRepository = partnerPreferenceRepository;
        this.masterValueResolver = masterValueResolver;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PartnerPreferenceResponse getMine(AuthenticatedUser principal) {
        UserProfile profile = requireProfile(authService.requireUsable(principal));
        return partnerPreferenceRepository.findByProfile(profile)
                .map(this::toResponse)
                .orElseGet(() -> new PartnerPreferenceResponse(
                        null, null, null, null, null, null, null, null, null, null, List.of()));
    }

    @Transactional
    public PartnerPreferenceResponse replaceMine(AuthenticatedUser principal, PartnerPreferenceRequest request) {
        UserProfile profile = requireProfile(authService.requireUsable(principal));
        validateRanges(request);
        MasterValue maritalStatus = resolveMaritalStatus(request.preferredMaritalStatusId());
        Map<ValueKey, MasterValue> desired = resolveValues(request.values());

        Instant now = Instant.now();
        PartnerPreference preference = partnerPreferenceRepository.findByProfile(profile).orElseGet(() -> {
            PartnerPreference created = new PartnerPreference();
            created.setProfile(profile);
            created.setCreatedAt(now);
            return created;
        });
        preference.setMinAge(toShort(request.minAge()));
        preference.setMaxAge(toShort(request.maxAge()));
        preference.setMinHeightCm(toShort(request.minHeightCm()));
        preference.setMaxHeightCm(toShort(request.maxHeightCm()));
        preference.setPreferredGender(blankToNull(request.preferredGender()));
        preference.setMinIncome(scaled(request.minIncome()));
        preference.setMaxIncome(scaled(request.maxIncome()));
        preference.setCurrencyCode(request.currencyCode() == null ? null : request.currencyCode().toUpperCase(Locale.ROOT));
        preference.setPreferredMaritalStatus(maritalStatus);
        preference.setLocationRadiusKm(request.locationRadiusKm());
        preference.setUpdatedAt(now);
        syncValues(preference, desired);
        return toResponse(partnerPreferenceRepository.save(preference));
    }

    /**
     * Applies only the difference. Clearing and re-adding would insert before deleting and trip the unique
     * (preference, type, value) constraint whenever a value is kept.
     */
    private void syncValues(PartnerPreference preference, Map<ValueKey, MasterValue> desired) {
        preference.getValues().removeIf(existing -> !desired.containsKey(keyOf(existing)));
        Set<ValueKey> kept = new HashSet<>();
        preference.getValues().forEach(existing -> kept.add(keyOf(existing)));
        desired.forEach((key, masterValue) -> {
            if (kept.contains(key)) {
                return;
            }
            PartnerPreferenceValue value = new PartnerPreferenceValue();
            value.setPreference(preference);
            value.setPreferenceType(key.type());
            value.setMasterValue(masterValue);
            preference.getValues().add(value);
        });
    }

    private Map<ValueKey, MasterValue> resolveValues(List<PreferenceValueRequest> requested) {
        Map<ValueKey, MasterValue> desired = new LinkedHashMap<>();
        if (requested == null || requested.isEmpty()) {
            return desired;
        }
        List<PreferenceType> types = new ArrayList<>();
        for (PreferenceValueRequest item : requested) {
            types.add(parseType(item.preferenceType()));
        }
        Map<Long, MasterValue> byId = new LinkedHashMap<>();
        masterValueResolver.requireAll(requested.stream().map(PreferenceValueRequest::masterValueId).toList())
                .forEach(value -> byId.put(value.getId(), value));
        for (int i = 0; i < requested.size(); i++) {
            PreferenceType type = types.get(i);
            MasterValue value = byId.get(requested.get(i).masterValueId());
            if (!type.categoryCode().equals(value.getCategory().getCode())) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        "Master value does not belong to the " + type + " preference type",
                        Map.of("preferenceType", type.name(), "masterValueId", value.getId())
                );
            }
            desired.putIfAbsent(new ValueKey(type, value.getId()), value);
        }
        return desired;
    }

    private PreferenceType parseType(String raw) {
        try {
            return PreferenceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Unsupported preferenceType",
                    Map.of("preferenceType", raw, "allowed", List.of(PreferenceType.values()))
            );
        }
    }

    private MasterValue resolveMaritalStatus(Long id) {
        MasterValue value = masterValueResolver.optional(id);
        if (value != null && !MARITAL_STATUS_CATEGORY.equals(value.getCategory().getCode())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "preferredMaritalStatusId must be a marital status",
                    Map.of("preferredMaritalStatusId", id)
            );
        }
        return value;
    }

    private void validateRanges(PartnerPreferenceRequest request) {
        int minimumAge = properties.getAuth().getMinAgeYears();
        if (request.minAge() != null && request.minAge() < minimumAge) {
            throw invalid("minAge must be at least " + minimumAge, "minAge", request.minAge());
        }
        if (request.maxAge() != null && request.maxAge() < minimumAge) {
            throw invalid("maxAge must be at least " + minimumAge, "maxAge", request.maxAge());
        }
        requireOrdered(request.minAge(), request.maxAge(), "minAge", "maxAge");
        requireOrdered(request.minHeightCm(), request.maxHeightCm(), "minHeightCm", "maxHeightCm");
        requireOrdered(request.minIncome(), request.maxIncome(), "minIncome", "maxIncome");
        if ((request.minIncome() != null || request.maxIncome() != null) && request.currencyCode() == null) {
            throw invalid("currencyCode is required when an income range is given", "currencyCode", null);
        }
    }

    private <T extends Comparable<T>> void requireOrdered(T min, T max, String minField, String maxField) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw invalid(minField + " must not be greater than " + maxField, minField, min);
        }
    }

    private ApiException invalid(String message, String field, Object value) {
        return new ApiException(
                ErrorCode.VALIDATION_ERROR,
                message,
                value == null ? Map.of("field", field) : Map.of("field", field, "value", value)
        );
    }

    private PartnerPreferenceResponse toResponse(PartnerPreference preference) {
        List<PreferenceValueResponse> values = preference.getValues().stream()
                .map(value -> new PreferenceValueResponse(
                        value.getPreferenceType().name(),
                        value.getMasterValue().getId(),
                        value.getMasterValue().getCode(),
                        value.getMasterValue().getName()))
                .sorted(Comparator.comparing(PreferenceValueResponse::preferenceType)
                        .thenComparing(PreferenceValueResponse::name))
                .toList();
        MasterValue marital = preference.getPreferredMaritalStatus();
        return new PartnerPreferenceResponse(
                toInteger(preference.getMinAge()),
                toInteger(preference.getMaxAge()),
                toInteger(preference.getMinHeightCm()),
                toInteger(preference.getMaxHeightCm()),
                preference.getPreferredGender(),
                preference.getMinIncome(),
                preference.getMaxIncome(),
                preference.getCurrencyCode(),
                marital == null ? null : marital.getId(),
                preference.getLocationRadiusKm(),
                values
        );
    }

    private UserProfile requireProfile(UserAccount user) {
        return userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
    }

    private static ValueKey keyOf(PartnerPreferenceValue value) {
        return new ValueKey(value.getPreferenceType(), value.getMasterValue().getId());
    }

    private static BigDecimal scaled(BigDecimal income) {
        return income == null ? null : income.setScale(2, RoundingMode.HALF_UP);
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private static Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ValueKey(PreferenceType type, Long masterValueId) {
    }
}
