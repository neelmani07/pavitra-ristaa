package com.pavitraristaa.discovery.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.discovery.entity.Recommendation;
import com.pavitraristaa.discovery.repository.DiscoveryProfileRepository;
import com.pavitraristaa.discovery.repository.DiscoverySpecifications;
import com.pavitraristaa.discovery.repository.RecommendationRepository;
import com.pavitraristaa.preference.service.CompatibilityScorer;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persisted recommendation snapshots, scored with the same CompatibilityScorer heuristic used for match
 * compatibility (preference module - shared rather than duplicated). Candidates are the same discoverable,
 * not-blocked pool browse() draws from, deliberately NOT narrowed by relationship mode or by excluding people
 * already interested/matched with - discovery has no dependency on connections/favorites, and adding one just
 * for this filter isn't worth the coupling for a first pass. A future pass can tighten this once there's a
 * concrete product reason to.
 */
@Service
public class RecommendationService {

    private static final int CANDIDATE_POOL_SIZE = 50;
    private static final int RESULT_SIZE = 20;
    private static final String ACTIVE = "ACTIVE";

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;
    private final DiscoveryProfileRepository discoveryProfileRepository;
    private final RecommendationRepository recommendationRepository;
    private final CompatibilityScorer compatibilityScorer;
    private final UserSummaryMapper userSummaryMapper;

    public RecommendationService(
            AuthService authService,
            UserProfileRepository userProfileRepository,
            DiscoveryProfileRepository discoveryProfileRepository,
            RecommendationRepository recommendationRepository,
            CompatibilityScorer compatibilityScorer,
            UserSummaryMapper userSummaryMapper
    ) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.discoveryProfileRepository = discoveryProfileRepository;
        this.recommendationRepository = recommendationRepository;
        this.compatibilityScorer = compatibilityScorer;
        this.userSummaryMapper = userSummaryMapper;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> list(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return recommendationRepository
                .findByUserAndStatusOrderByScoreDescGeneratedAtDesc(self, ACTIVE, PaginationSupport.pageable(page, size))
                .map(recommendation -> userSummaryMapper.toSummary(recommendation.getRecommendedProfile()))
                .getContent();
    }

    @Transactional
    public int refresh(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        UserProfile selfProfile = requireProfile(self);
        recommendationRepository.deleteByUser(self);

        Specification<UserProfile> spec = Specification.allOf(
                DiscoverySpecifications.discoverableBy(self),
                DiscoverySpecifications.notBlockedEitherWayWith(self));
        var candidatePool = discoveryProfileRepository.findAll(
                spec, PageRequest.of(0, CANDIDATE_POOL_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt")));

        Instant now = Instant.now();
        List<Recommendation> ranked = candidatePool.getContent().stream()
                .map(candidate -> score(self, selfProfile, candidate, now))
                .sorted(Comparator.comparing(Recommendation::getScore).reversed())
                .limit(RESULT_SIZE)
                .toList();
        recommendationRepository.saveAll(ranked);
        return ranked.size();
    }

    private Recommendation score(UserAccount self, UserProfile selfProfile, UserProfile candidate, Instant now) {
        CompatibilityScorer.Score score = compatibilityScorer.score(selfProfile, candidate);
        Recommendation recommendation = new Recommendation();
        recommendation.setUser(self);
        recommendation.setRecommendedProfile(candidate);
        recommendation.setScore(BigDecimal.valueOf(score.score()));
        recommendation.setReason(reasonFrom(score));
        recommendation.setGeneratedAt(now);
        recommendation.setStatus(ACTIVE);
        return recommendation;
    }

    private String reasonFrom(CompatibilityScorer.Score score) {
        return score.factors().stream()
                .filter(CompatibilityScorer.Factor::matched)
                .map(CompatibilityScorer.Factor::label)
                .findFirst()
                .orElse("Recommended for you");
    }

    private UserProfile requireProfile(UserAccount user) {
        return userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
    }
}
