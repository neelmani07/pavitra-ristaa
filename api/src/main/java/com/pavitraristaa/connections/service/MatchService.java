package com.pavitraristaa.connections.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.AgeCalculator;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.connections.dto.CompatibilityResponse;
import com.pavitraristaa.connections.dto.CompatibilityResponse.CompatibilityFactor;
import com.pavitraristaa.connections.dto.MatchResponse;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.entity.MatchStatus;
import com.pavitraristaa.connections.repository.MatchRepository;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.preference.entity.PartnerPreference;
import com.pavitraristaa.preference.entity.PartnerPreferenceValue;
import com.pavitraristaa.preference.entity.PreferenceType;
import com.pavitraristaa.preference.repository.PartnerPreferenceRepository;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final AuthService authService;
    private final MatchRepository matchRepository;
    private final UserProfileRepository userProfileRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final UserSummaryMapper userSummaryMapper;

    public MatchService(
            AuthService authService,
            MatchRepository matchRepository,
            UserProfileRepository userProfileRepository,
            PartnerPreferenceRepository partnerPreferenceRepository,
            UserSummaryMapper userSummaryMapper
    ) {
        this.authService = authService;
        this.matchRepository = matchRepository;
        this.userProfileRepository = userProfileRepository;
        this.partnerPreferenceRepository = partnerPreferenceRepository;
        this.userSummaryMapper = userSummaryMapper;
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
        }
        return toResponse(match, self);
    }

    @Transactional(readOnly = true)
    public CompatibilityResponse compatibility(AuthenticatedUser principal, UUID matchId) {
        UserAccount self = authService.requireUsable(principal);
        Match match = requireParticipant(self, matchId);
        UserProfile selfProfile = requireProfile(self);
        UserProfile otherProfile = requireProfile(match.other(self));
        Score score = scoreOf(selfProfile, otherProfile);
        return new CompatibilityResponse(match.getUuid(), score.score(), score.factors());
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

    /**
     * A first-pass heuristic, not a machine-learned model - see CompatibilityResponse. Deliberately does not
     * consider height, religion or marital status: the schema records those only as partner PREFERENCES
     * (partner_preference.min/max_height_cm, partner_preference_value RELIGION/MARITAL_STATUS), never as a
     * value on the person's own profile, so there is nothing on either side to compare a preference against.
     */
    private Score scoreOf(UserProfile a, UserProfile b) {
        List<CompatibilityFactor> factors = new ArrayList<>();
        int earned = 0;
        int possible = 0;

        Optional<PartnerPreference> prefA = partnerPreferenceRepository.findByProfile(a);
        Optional<PartnerPreference> prefB = partnerPreferenceRepository.findByProfile(b);

        Integer ageA = AgeCalculator.fromDateOfBirth(a.getDateOfBirth());
        Integer ageB = AgeCalculator.fromDateOfBirth(b.getDateOfBirth());
        if (prefA.isPresent() && ageB != null) {
            possible += 10;
            boolean inRange = inRange(ageB, prefA.get().getMinAge(), prefA.get().getMaxAge());
            earned += inRange ? 10 : 0;
            factors.add(new CompatibilityFactor("AGE_A_TO_B", "Their age fits your preference", inRange, ageB + " years"));
        }
        if (prefB.isPresent() && ageA != null) {
            possible += 10;
            boolean inRange = inRange(ageA, prefB.get().getMinAge(), prefB.get().getMaxAge());
            earned += inRange ? 10 : 0;
            factors.add(new CompatibilityFactor("AGE_B_TO_A", "Your age fits their preference", inRange, ageA + " years"));
        }

        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.DIET,
                a.getLifestyle() == null ? null : a.getLifestyle().getDiet(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDiet(), "DIET");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getDiet(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDiet());
        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.SMOKING,
                a.getLifestyle() == null ? null : a.getLifestyle().getSmoking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getSmoking(), "SMOKING");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getSmoking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getSmoking());
        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.DRINKING,
                a.getLifestyle() == null ? null : a.getLifestyle().getDrinking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDrinking(), "DRINKING");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getDrinking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDrinking());

        Set<Long> interestsA = idsOf(a.getInterests().stream().map(i -> i.getInterest()).toList());
        Set<Long> interestsB = idsOf(b.getInterests().stream().map(i -> i.getInterest()).toList());
        if (!interestsA.isEmpty() && !interestsB.isEmpty()) {
            possible += 20;
            long shared = interestsA.stream().filter(interestsB::contains).count();
            int points = (int) Math.min(20, shared * 4);
            earned += points;
            factors.add(new CompatibilityFactor("SHARED_INTERESTS", "Shared interests", shared > 0, shared + " in common"));
        }

        Set<Long> hobbiesA = idsOf(a.getHobbies().stream().map(h -> h.getHobby()).toList());
        Set<Long> hobbiesB = idsOf(b.getHobbies().stream().map(h -> h.getHobby()).toList());
        if (!hobbiesA.isEmpty() && !hobbiesB.isEmpty()) {
            possible += 20;
            long shared = hobbiesA.stream().filter(hobbiesB::contains).count();
            int points = (int) Math.min(20, shared * 4);
            earned += points;
            factors.add(new CompatibilityFactor("SHARED_HOBBIES", "Shared hobbies", shared > 0, shared + " in common"));
        }

        if (a.getCity() != null && b.getCity() != null) {
            possible += 15;
            boolean sameCity = a.getCity().getId().equals(b.getCity().getId());
            earned += sameCity ? 15 : 0;
            factors.add(new CompatibilityFactor("LOCATION", "Same city", sameCity,
                    sameCity ? "Both in " + a.getCity().getName() : "Different cities"));
        }

        int score = possible == 0 ? 50 : Math.round(100f * earned / possible);
        return new Score(score, factors);
    }

    private int lifestylePossible(Optional<PartnerPreference> prefA, Optional<PartnerPreference> prefB,
            MasterValue actualA, MasterValue actualB) {
        int possible = 0;
        if (prefA.isPresent() && actualB != null) {
            possible += 5;
        }
        if (prefB.isPresent() && actualA != null) {
            possible += 5;
        }
        return possible;
    }

    private int lifestyleFactor(
            Optional<PartnerPreference> prefA, Optional<PartnerPreference> prefB,
            List<CompatibilityFactor> factors, PreferenceType type,
            MasterValue actualA, MasterValue actualB, String label
    ) {
        int earned = 0;
        if (prefA.isPresent() && actualB != null) {
            boolean matched = preferredValues(prefA.get(), type).contains(actualB.getId());
            earned += matched ? 5 : 0;
            factors.add(new CompatibilityFactor(label + "_A_TO_B", "Their " + label.toLowerCase() + " fits your preference",
                    matched, actualB.getName()));
        }
        if (prefB.isPresent() && actualA != null) {
            boolean matched = preferredValues(prefB.get(), type).contains(actualA.getId());
            earned += matched ? 5 : 0;
            factors.add(new CompatibilityFactor(label + "_B_TO_A", "Your " + label.toLowerCase() + " fits their preference",
                    matched, actualA.getName()));
        }
        return earned;
    }

    private Set<Long> preferredValues(PartnerPreference preference, PreferenceType type) {
        return preference.getValues().stream()
                .filter(value -> value.getPreferenceType() == type)
                .map(PartnerPreferenceValue::getMasterValue)
                .map(MasterValue::getId)
                .collect(Collectors.toSet());
    }

    private boolean inRange(int value, Short min, Short max) {
        if (min != null && value < min) {
            return false;
        }
        return max == null || value <= max;
    }

    private Set<Long> idsOf(List<MasterValue> values) {
        return values.stream().map(MasterValue::getId).collect(Collectors.toSet());
    }

    private record Score(int score, List<CompatibilityFactor> factors) {
    }
}
