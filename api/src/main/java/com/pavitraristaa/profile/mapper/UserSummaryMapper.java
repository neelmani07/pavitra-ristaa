package com.pavitraristaa.profile.mapper;

import com.pavitraristaa.common.util.AgeCalculator;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.media.service.MediaUrlResolver;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.entity.PhotoApprovalStatus;
import com.pavitraristaa.profile.entity.PhotoVisibility;
import com.pavitraristaa.profile.entity.ProfilePhoto;
import com.pavitraristaa.profile.entity.ProfileVerification;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.relationship.entity.UserRelationshipMode;
import com.pavitraristaa.relationship.repository.UserRelationshipModeRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds the small profile card (UserSummaryResponse) shown in discovery results, favorites, interests and
 * matches - as opposed to ProfileMapper, which builds the full profile for /me/profile and /profiles/{id}.
 */
@Component
public class UserSummaryMapper {

    private final MediaUrlResolver mediaUrlResolver;
    private final UserRelationshipModeRepository userRelationshipModeRepository;

    public UserSummaryMapper(MediaUrlResolver mediaUrlResolver, UserRelationshipModeRepository userRelationshipModeRepository) {
        this.mediaUrlResolver = mediaUrlResolver;
        this.userRelationshipModeRepository = userRelationshipModeRepository;
    }

    public UserSummaryResponse toSummary(UserProfile profile) {
        List<String> relationshipModes = userRelationshipModeRepository.findByUser(profile.getUser()).stream()
                .map(UserRelationshipMode::getRelationshipMode)
                .map(mode -> mode.getCode())
                .toList();
        return new UserSummaryResponse(
                profile.getUser().getUuid(),
                displayName(profile),
                AgeCalculator.fromDateOfBirth(profile.getDateOfBirth()),
                profile.getGender(),
                location(profile),
                primaryPhotoUrl(profile),
                relationshipModes,
                isVerified(profile.getVerification())
        );
    }

    private String displayName(UserProfile profile) {
        return profile.getDisplayName() != null ? profile.getDisplayName() : profile.getFirstName();
    }

    private String primaryPhotoUrl(UserProfile profile) {
        return profile.getPhotos().stream()
                .filter(ProfilePhoto::isPrimary)
                .filter(photo -> photo.getVisibility() != PhotoVisibility.PRIVATE)
                .filter(photo -> photo.getApprovalStatus() == PhotoApprovalStatus.APPROVED)
                .findFirst()
                .map(photo -> mediaUrlResolver.urlFor(photo.getMediaFile()))
                .orElse(null);
    }

    private boolean isVerified(ProfileVerification verification) {
        return verification != null && "VERIFIED".equals(verification.getVerificationStatus());
    }

    private Map<String, Object> location(UserProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("country", master(profile.getCountry()));
        map.put("state", master(profile.getState()));
        map.put("city", master(profile.getCity()));
        return map;
    }

    private Map<String, Object> master(MasterValue value) {
        if (value == null) {
            return null;
        }
        return Map.of("id", value.getId(), "code", value.getCode(), "name", value.getName());
    }
}
