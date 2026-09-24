package com.pavitraristaa.connections.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.connections.dto.CompatibilityResponse;
import com.pavitraristaa.connections.dto.CompatibilityResponse.CompatibilityFactor;
import com.pavitraristaa.connections.dto.MatchResponse;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.entity.MatchStatus;
import com.pavitraristaa.connections.event.MatchEndedEvent;
import com.pavitraristaa.connections.repository.MatchRepository;
import com.pavitraristaa.preference.service.CompatibilityScorer;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final AuthService authService;
    private final MatchRepository matchRepository;
    private final UserProfileRepository userProfileRepository;
    private final CompatibilityScorer compatibilityScorer;
    private final UserSummaryMapper userSummaryMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(
            AuthService authService,
            MatchRepository matchRepository,
            UserProfileRepository userProfileRepository,
            CompatibilityScorer compatibilityScorer,
            UserSummaryMapper userSummaryMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.authService = authService;
        this.matchRepository = matchRepository;
        this.userProfileRepository = userProfileRepository;
        this.compatibilityScorer = compatibilityScorer;
        this.userSummaryMapper = userSummaryMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listActive(AuthenticatedUser principal, Integer page, Integer size) {
        return list(principal, MatchStatus.ACTIVE, page, size);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listHistory(AuthenticatedUser principal, Integer page, Integer size) {
        return list(principal, MatchStatus.UNMATCHED, page, size);
    }

    private List<MatchResponse> list(AuthenticatedUser principal, MatchStatus status, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return matchRepository.findByUserAndStatus(self, status, PaginationSupport.pageable(page, size))
                .map(match -> toResponse(match, self))
                .getContent();
    }

    @Transactional(readOnly = true)
    public MatchResponse getOne(AuthenticatedUser principal, UUID matchId) {
        UserAccount self = authService.requireUsable(principal);
        Match match = requireParticipant(self, matchId);
        return toResponse(match, self);
    }

    @Transactional
    public MatchResponse unmatch(AuthenticatedUser principal, UUID matchId) {
        UserAccount self = authService.requireUsable(principal);
        Match match = requireParticipant(self, matchId);
        if (match.getStatus() == MatchStatus.ACTIVE) {
            match.setStatus(MatchStatus.UNMATCHED);
            match.setUnmatchedAt(Instant.now());
            matchRepository.save(match);
            eventPublisher.publishEvent(new MatchEndedEvent(match));
        }
        return toResponse(match, self);
    }

    @Transactional(readOnly = true)
    public CompatibilityResponse compatibility(AuthenticatedUser principal, UUID matchId) {
        UserAccount self = authService.requireUsable(principal);
        Match match = requireParticipant(self, matchId);
        UserProfile selfProfile = requireProfile(self);
        UserProfile otherProfile = requireProfile(match.other(self));
        CompatibilityScorer.Score score = compatibilityScorer.score(selfProfile, otherProfile);
        List<CompatibilityFactor> factors = score.factors().stream()
                .map(f -> new CompatibilityFactor(f.key(), f.label(), f.matched(), f.detail()))
                .toList();
        return new CompatibilityResponse(match.getUuid(), score.score(), factors);
    }

    private Match requireParticipant(UserAccount self, UUID matchId) {
        Match match = matchRepository.findByUuid(matchId)
                .orElseThrow(() -> new ApiException(ErrorCode.MATCH_NOT_FOUND, "Match not found"));
        if (!match.involves(self)) {
            throw new ApiException(ErrorCode.MATCH_NOT_FOUND, "Match not found");
        }
        return match;
    }

    private UserProfile requireProfile(UserAccount user) {
        return userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
    }

    private MatchResponse toResponse(Match match, UserAccount self) {
        UserProfile otherProfile = requireProfile(match.other(self));
        return new MatchResponse(
                match.getUuid(),
                userSummaryMapper.toSummary(otherProfile),
                match.getRelationshipMode().getCode(),
                match.getStatus().name(),
                match.getMatchedAt()
        );
    }
}
