package com.pavitraristaa.connections.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.connections.dto.CreateInterestRequest;
import com.pavitraristaa.connections.dto.InterestResponse;
import com.pavitraristaa.connections.entity.Interest;
import com.pavitraristaa.connections.entity.InterestStatus;
import com.pavitraristaa.connections.entity.InterestStatusHistory;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.entity.MatchStatus;
import com.pavitraristaa.connections.event.MatchActivatedEvent;
import com.pavitraristaa.connections.repository.InterestRepository;
import com.pavitraristaa.connections.repository.InterestStatusHistoryRepository;
import com.pavitraristaa.connections.repository.MatchRepository;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.relationship.entity.RelationshipMode;
import com.pavitraristaa.relationship.repository.RelationshipModeRepository;
import com.pavitraristaa.trust.repository.BlockRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterestService {

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;
    private final InterestRepository interestRepository;
    private final InterestStatusHistoryRepository interestStatusHistoryRepository;
    private final MatchRepository matchRepository;
    private final RelationshipModeRepository relationshipModeRepository;
    private final BlockRepository blockRepository;
    private final UserSummaryMapper userSummaryMapper;
    private final ApplicationEventPublisher eventPublisher;

    public InterestService(
            AuthService authService,
            UserProfileRepository userProfileRepository,
            InterestRepository interestRepository,
            InterestStatusHistoryRepository interestStatusHistoryRepository,
            MatchRepository matchRepository,
            RelationshipModeRepository relationshipModeRepository,
            BlockRepository blockRepository,
            UserSummaryMapper userSummaryMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.interestRepository = interestRepository;
        this.interestStatusHistoryRepository = interestStatusHistoryRepository;
        this.matchRepository = matchRepository;
        this.relationshipModeRepository = relationshipModeRepository;
        this.blockRepository = blockRepository;
        this.userSummaryMapper = userSummaryMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InterestResponse send(AuthenticatedUser principal, CreateInterestRequest request) {
        UserAccount self = authService.requireUsable(principal);
        if (self.getUuid().equals(request.profileId())) {
            throw new ApiException(ErrorCode.CANNOT_INTERACT_WITH_SELF, "You cannot send an interest to yourself");
        }
        UserProfile targetProfile = userProfileRepository.findByUser_UuidAndDeletedFalse(request.profileId())
                .filter(profile -> profile.getProfileStatus() == ProfileStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        UserAccount receiver = targetProfile.getUser();
        if (blockRepository.existsEitherDirection(self, receiver)) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "You cannot interact with this user");
        }
        RelationshipMode mode = relationshipModeRepository.findByCodeAndActiveTrue(
                        request.relationshipMode().trim().toUpperCase())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Unknown relationship mode",
                        Map.of("relationshipMode", request.relationshipMode())));
        if (interestRepository.existsBySenderAndReceiverAndRelationshipModeAndStatus(
                self, receiver, mode, InterestStatus.PENDING)) {
            throw new ApiException(ErrorCode.ALREADY_INTERESTED, "An interest is already pending for this mode");
        }

        Interest interest = new Interest();
        interest.setUuid(UUID.randomUUID());
        interest.setSender(self);
        interest.setReceiver(receiver);
        interest.setRelationshipMode(mode);
        interest.setStatus(InterestStatus.PENDING);
        interest.setMessage(blankToNull(request.message()));
        interest.setCreatedAt(Instant.now());
        return toResponse(interestRepository.save(interest));
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> listSent(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return interestRepository.findBySenderOrderByCreatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> listReceived(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return interestRepository.findByReceiverOrderByCreatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public InterestResponse getOne(AuthenticatedUser principal, UUID interestId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireParticipant(self, interestId));
    }

    @Transactional
    public InterestResponse accept(AuthenticatedUser principal, UUID interestId) {
        UserAccount self = authService.requireUsable(principal);
        Interest interest = requireParticipant(self, interestId);
        requireRole(interest, self, interest.getReceiver(), "Only the receiver can accept an interest");
        requirePending(interest);

        transition(interest, InterestStatus.ACCEPTED, self);
        createOrReviveMatch(interest);
        return toResponse(interest);
    }

    @Transactional
    public InterestResponse decline(AuthenticatedUser principal, UUID interestId) {
        UserAccount self = authService.requireUsable(principal);
        Interest interest = requireParticipant(self, interestId);
        requireRole(interest, self, interest.getReceiver(), "Only the receiver can decline an interest");
        requirePending(interest);

        transition(interest, InterestStatus.DECLINED, self);
        return toResponse(interest);
    }

    @Transactional
    public InterestResponse withdraw(AuthenticatedUser principal, UUID interestId) {
        UserAccount self = authService.requireUsable(principal);
        Interest interest = requireParticipant(self, interestId);
        requireRole(interest, self, interest.getSender(), "Only the sender can withdraw an interest");
        requirePending(interest);

        transition(interest, InterestStatus.WITHDRAWN, self);
        return toResponse(interest);
    }

    private void createOrReviveMatch(Interest interest) {
        UserAccount lower = interest.getSender().getId() < interest.getReceiver().getId()
                ? interest.getSender() : interest.getReceiver();
        UserAccount higher = lower == interest.getSender() ? interest.getReceiver() : interest.getSender();
        Match match = matchRepository.findByUserAAndUserBAndRelationshipMode(lower, higher, interest.getRelationshipMode())
                .orElseGet(Match::new);
        Instant now = Instant.now();
        match.setUserA(lower);
        match.setUserB(higher);
        match.setRelationshipMode(interest.getRelationshipMode());
        match.setSourceInterest(interest);
        if (match.getUuid() == null) {
            match.setUuid(UUID.randomUUID());
        }
        match.setStatus(MatchStatus.ACTIVE);
        match.setMatchedAt(now);
        match.setUnmatchedAt(null);
        matchRepository.save(match);
        eventPublisher.publishEvent(new MatchActivatedEvent(match));
    }

    private void transition(Interest interest, InterestStatus newStatus, UserAccount changedBy) {
        InterestStatus oldStatus = interest.getStatus();
        interest.setStatus(newStatus);
        interest.setRespondedAt(Instant.now());
        interestRepository.save(interest);
        InterestStatusHistory history = new InterestStatusHistory();
        history.setInterest(interest);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangedAt(Instant.now());
        interestStatusHistoryRepository.save(history);
    }

    private Interest requireParticipant(UserAccount self, UUID interestId) {
        Interest interest = interestRepository.findByUuid(interestId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Interest not found"));
        boolean participant = interest.getSender().getId().equals(self.getId())
                || interest.getReceiver().getId().equals(self.getId());
        if (!participant) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Interest not found");
        }
        return interest;
    }

    private void requireRole(Interest interest, UserAccount self, UserAccount requiredRole, String message) {
        if (!requiredRole.getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, message);
        }
    }

    private void requirePending(Interest interest) {
        if (interest.getStatus() != InterestStatus.PENDING) {
            throw new ApiException(
                    ErrorCode.INTEREST_NOT_ACTIONABLE,
                    "This interest is no longer pending",
                    Map.of("status", interest.getStatus().name()));
        }
    }

    private InterestResponse toResponse(Interest interest) {
        return new InterestResponse(
                interest.getUuid(),
                summaryOf(interest.getSender()),
                summaryOf(interest.getReceiver()),
                interest.getRelationshipMode().getCode(),
                interest.getStatus().name(),
                interest.getMessage(),
                interest.getCreatedAt(),
                interest.getRespondedAt()
        );
    }

    private UserSummaryResponse summaryOf(UserAccount account) {
        UserProfile profile = userProfileRepository.findByUserAndDeletedFalse(account)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        return userSummaryMapper.toSummary(profile);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
