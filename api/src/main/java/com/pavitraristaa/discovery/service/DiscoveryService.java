package com.pavitraristaa.discovery.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.discovery.dto.DiscoverySearchRequest;
import com.pavitraristaa.discovery.dto.HomeResponse;
import com.pavitraristaa.discovery.dto.SpiritualSearchFilterRequest;
import com.pavitraristaa.discovery.entity.ProfileView;
import com.pavitraristaa.discovery.repository.DiscoveryProfileRepository;
import com.pavitraristaa.discovery.repository.DiscoverySpecifications;
import com.pavitraristaa.discovery.repository.ProfileViewRepository;
import com.pavitraristaa.profile.dto.ProfileCompletionResponse;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.profile.service.ProfileService;
import com.pavitraristaa.relationship.repository.RelationshipModeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {

    private final AuthService authService;
    private final ProfileService profileService;
    private final UserProfileRepository userProfileRepository;
    private final DiscoveryProfileRepository discoveryProfileRepository;
    private final ProfileViewRepository profileViewRepository;
    private final RelationshipModeRepository relationshipModeRepository;
    private final UserSummaryMapper userSummaryMapper;

    public DiscoveryService(
            AuthService authService,
            ProfileService profileService,
            UserProfileRepository userProfileRepository,
            DiscoveryProfileRepository discoveryProfileRepository,
            ProfileViewRepository profileViewRepository,
            RelationshipModeRepository relationshipModeRepository,
            UserSummaryMapper userSummaryMapper
    ) {
        this.authService = authService;
        this.profileService = profileService;
        this.userProfileRepository = userProfileRepository;
        this.discoveryProfileRepository = discoveryProfileRepository;
        this.profileViewRepository = profileViewRepository;
        this.relationshipModeRepository = relationshipModeRepository;
        this.userSummaryMapper = userSummaryMapper;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> browse(
            AuthenticatedUser principal, String mode, Integer minAge, Integer maxAge,
            Long countryId, Long stateId, Long cityId,
            String spiritualCommunity, String spiritualInterest, String practice, String anySpiritualProfession,
            Integer page, Integer size
    ) {
        UserAccount self = authService.requireUsable(principal);
        Specification<UserProfile> spec = combine(
                baseSpec(self, mode == null ? List.of() : List.of(mode)),
                DiscoverySpecifications.countryIs(countryId),
                DiscoverySpecifications.stateIs(stateId),
                DiscoverySpecifications.cityIs(cityId),
                DiscoverySpecifications.bornOnOrBefore(dobOnOrBefore(minAge)),
                DiscoverySpecifications.bornOnOrAfter(dobOnOrAfter(maxAge)),
                DiscoverySpecifications.spiritualFieldContains("spiritualCommunity", spiritualCommunity),
                DiscoverySpecifications.spiritualFieldContains("spiritualInterests", spiritualInterest),
                DiscoverySpecifications.spiritualFieldContains("practices", practice),
                DiscoverySpecifications.spiritualFieldContains("anySpiritualProfession", anySpiritualProfession));
        return runSearch(spec, page, size);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> search(AuthenticatedUser principal, DiscoverySearchRequest request) {
        UserAccount self = authService.requireUsable(principal);
        SpiritualSearchFilterRequest spiritual = request.spiritual();
        Specification<UserProfile> spec = combine(
                baseSpec(self, request.relationshipModes() == null ? List.of() : request.relationshipModes()),
                DiscoverySpecifications.countryIs(request.countryId()),
                DiscoverySpecifications.stateIs(request.stateId()),
                DiscoverySpecifications.cityIs(request.cityId()),
                DiscoverySpecifications.bornOnOrBefore(dobOnOrBefore(request.minAge())),
                DiscoverySpecifications.bornOnOrAfter(dobOnOrAfter(request.maxAge())),
                DiscoverySpecifications.textMatches(request.query()),
                spiritual == null ? null
                        : DiscoverySpecifications.spiritualFieldContains("spiritualCommunity", spiritual.spiritualCommunity()),
                spiritual == null ? null
                        : DiscoverySpecifications.spiritualFieldContains("spiritualInterests", spiritual.spiritualInterest()),
                spiritual == null ? null
                        : DiscoverySpecifications.spiritualFieldContains("practices", spiritual.practice()),
                spiritual == null ? null
                        : DiscoverySpecifications.spiritualFieldContains(
                                "anySpiritualProfession", spiritual.anySpiritualProfession()));
        return runSearch(spec, request.page(), request.size());
    }

    private Specification<UserProfile> baseSpec(UserAccount self, List<String> modeCodes) {
        return Specification.allOf(
                DiscoverySpecifications.discoverableBy(self),
                DiscoverySpecifications.hasAnyRelationshipMode(resolveModes(modeCodes)),
                DiscoverySpecifications.notBlockedEitherWayWith(self)
        );
    }

    /** Specification.and(null) throws rather than no-op, so optional filters are combined this way instead. */
    @SafeVarargs
    private Specification<UserProfile> combine(Specification<UserProfile> base, Specification<UserProfile>... optional) {
        Specification<UserProfile> result = base;
        for (Specification<UserProfile> spec : optional) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    private List<UserSummaryResponse> runSearch(Specification<UserProfile> spec, Integer page, Integer size) {
        var unsorted = PaginationSupport.pageable(page, size);
        var sorted = org.springframework.data.domain.PageRequest.of(
                unsorted.getPageNumber(), unsorted.getPageSize(), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<UserProfile> results = discoveryProfileRepository.findAll(spec, sorted);
        return results.map(userSummaryMapper::toSummary).getContent();
    }

    @Transactional
    public void recordView(AuthenticatedUser principal, UUID profileId) {
        UserAccount viewer = authService.requireUsable(principal);
        UserProfile viewed = requireViewable(viewer, profileId);
        ProfileView view = new ProfileView();
        view.setViewer(viewer);
        view.setViewedProfile(viewed);
        view.setViewedAt(Instant.now());
        profileViewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> recentlyViewed(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount viewer = authService.requireUsable(principal);
        return profileViewRepository.findMostRecentDistinctByViewer(viewer, PaginationSupport.pageable(page, size))
                .map(view -> userSummaryMapper.toSummary(view.getViewedProfile()))
                .getContent();
    }

    @Transactional(readOnly = true)
    public HomeResponse home(AuthenticatedUser principal) {
        ProfileCompletionResponse completion = profileService.completion(principal);
        List<UserSummaryResponse> discover = browse(
                principal, null, null, null, null, null, null, null, null, null, null, 0, 10);
        return new HomeResponse(completion.percent(), completion.missingSections(), discover);
    }

    /** Not viewable if the profile doesn't exist, isn't published, or belongs to a user who blocked/was blocked. */
    private UserProfile requireViewable(UserAccount viewer, UUID profileId) {
        UserProfile target = userProfileRepository.findByUser_UuidAndDeletedFalse(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        if (viewer.getUuid().equals(profileId)) {
            throw new ApiException(ErrorCode.CANNOT_INTERACT_WITH_SELF, "You cannot view your own profile this way");
        }
        if (target.getProfileStatus() != com.pavitraristaa.profile.entity.ProfileStatus.ACTIVE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found");
        }
        return target;
    }

    /** modeCodes empty/null means "no filter" - resolved to every active mode so the query's non-empty IN list stays safe. */
    private List<String> resolveModes(List<String> modeCodes) {
        List<String> requested = modeCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (requested.isEmpty()) {
            return relationshipModeRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                    .map(mode -> mode.getCode())
                    .toList();
        }
        List<String> resolved = relationshipModeRepository.findByCodeInAndActiveTrue(requested).stream()
                .map(mode -> mode.getCode())
                .toList();
        if (resolved.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unknown relationship mode", java.util.Map.of("mode", requested));
        }
        return resolved;
    }

    private LocalDate dobOnOrBefore(Integer minAge) {
        return minAge == null ? null : LocalDate.now(ZoneOffset.UTC).minusYears(minAge);
    }

    private LocalDate dobOnOrAfter(Integer maxAge) {
        return maxAge == null ? null : LocalDate.now(ZoneOffset.UTC).minusYears(maxAge);
    }
}
