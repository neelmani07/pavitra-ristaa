package com.pavitraristaa.profile.mapper;

import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.media.service.MediaUrlResolver;
import com.pavitraristaa.profile.dto.ProfilePhotoResponse;
import com.pavitraristaa.profile.dto.ProfileResponse;
import com.pavitraristaa.profile.dto.SpiritualProfileResponse;
import com.pavitraristaa.profile.entity.PhotoApprovalStatus;
import com.pavitraristaa.profile.entity.PhotoVisibility;
import com.pavitraristaa.profile.entity.ProfileCareer;
import com.pavitraristaa.profile.entity.ProfileEducation;
import com.pavitraristaa.profile.entity.ProfileFamily;
import com.pavitraristaa.profile.entity.ProfileHobby;
import com.pavitraristaa.profile.entity.ProfileInterest;
import com.pavitraristaa.profile.entity.ProfileLanguage;
import com.pavitraristaa.profile.entity.ProfileLifestyle;
import com.pavitraristaa.profile.entity.ProfilePhoto;
import com.pavitraristaa.profile.entity.ProfileVerification;
import com.pavitraristaa.profile.entity.SpiritualProfile;
import com.pavitraristaa.profile.entity.UserProfile;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    private final MediaUrlResolver mediaUrlResolver;

    public ProfileMapper(MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public ProfileResponse toResponse(UserProfile profile, List<String> relationshipModes, boolean owner) {
        List<ProfilePhotoResponse> photos = profile.getPhotos().stream()
                .sorted(Comparator.comparingInt(ProfilePhoto::getDisplayOrder))
                .filter(photo -> owner || isPubliclyVisible(photo))
                .map(this::toPhoto)
                .toList();
        return new ProfileResponse(
                profile.getUser().getUuid(),
                profile.getUser().getUuid(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDisplayName(),
                profile.getDateOfBirth(),
                age(profile.getDateOfBirth()),
                profile.getGender(),
                profile.getHeadline(),
                profile.getAboutMe(),
                profile.getProfileStatus().name(),
                profile.getProfileCompletionPercent(),
                location(profile),
                relationshipModes,
                education(profile.getEducation()),
                career(profile.getCareer()),
                family(profile.getFamily()),
                lifestyle(profile.getLifestyle()),
                languages(profile.getLanguages()),
                interests(profile.getInterests()),
                hobbies(profile.getHobbies()),
                spiritual(profile.getSpiritualProfile()),
                photos,
                verification(profile.getVerification())
        );
    }

    /**
     * A photo is shown to someone other than its owner only once a moderator has approved it, and never if the
     * owner marked it PRIVATE. A photo awaiting review or one a moderator rejected must never be public.
     */
    private boolean isPubliclyVisible(ProfilePhoto photo) {
        return photo.getVisibility() != PhotoVisibility.PRIVATE
                && photo.getApprovalStatus() == PhotoApprovalStatus.APPROVED;
    }

    public ProfilePhotoResponse toPhoto(ProfilePhoto photo) {
        String url = mediaUrlResolver.urlFor(photo.getMediaFile());
        return new ProfilePhotoResponse(
                photo.getUuid(),
                url,
                photo.getPhotoType().name(),
                photo.getDisplayOrder(),
                photo.isPrimary(),
                photo.getVisibility().name(),
                photo.getApprovalStatus().name()
        );
    }

    public SpiritualProfileResponse spiritual(SpiritualProfile spiritual) {
        if (spiritual == null) {
            return null;
        }
        return new SpiritualProfileResponse(
                spiritual.getSpiritualCommunity(),
                spiritual.getSpiritualInterests(),
                spiritual.getPractices(),
                spiritual.getAnySpiritualProfession(),
                spiritual.getDreamSpiritualPilgrimageDestination(),
                spiritual.getCreatedAt(),
                spiritual.getUpdatedAt()
        );
    }

    public Map<String, Object> education(ProfileEducation education) {
        if (education == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("educationLevel", master(education.getEducationLevel()));
        map.put("institutionName", education.getInstitutionName());
        map.put("fieldOfStudy", education.getFieldOfStudy());
        map.put("graduationYear", education.getGraduationYear());
        map.put("additionalDetails", education.getAdditionalDetails());
        return map;
    }

    public Map<String, Object> career(ProfileCareer career) {
        if (career == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("occupation", master(career.getOccupation()));
        map.put("jobTitle", career.getJobTitle());
        map.put("companyName", career.getCompanyName());
        map.put("industry", master(career.getIndustry()));
        map.put("workLocationCity", master(career.getWorkLocationCity()));
        map.put("experienceYears", career.getExperienceYears());
        map.put("isEmployed", career.isEmployed());
        return map;
    }

    public Map<String, Object> family(ProfileFamily family) {
        if (family == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("familyType", family.getFamilyType());
        map.put("parentsStatus", family.getParentsStatus());
        map.put("siblingsCount", family.getSiblingsCount());
        map.put("familyDescription", family.getFamilyDescription());
        map.put("familyValues", family.getFamilyValues());
        return map;
    }

    public Map<String, Object> lifestyle(ProfileLifestyle lifestyle) {
        if (lifestyle == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("diet", master(lifestyle.getDiet()));
        map.put("smoking", master(lifestyle.getSmoking()));
        map.put("drinking", master(lifestyle.getDrinking()));
        map.put("exerciseFrequency", master(lifestyle.getExerciseFrequency()));
        map.put("sleepPattern", lifestyle.getSleepPattern());
        map.put("pets", lifestyle.getPets());
        return map;
    }

    public List<Map<String, Object>> languages(List<ProfileLanguage> languages) {
        return languages.stream().map(language -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("language", master(language.getLanguage()));
            map.put("proficiency", language.getProficiency());
            map.put("isPrimary", language.isPrimary());
            return map;
        }).toList();
    }

    public List<Map<String, Object>> interests(List<ProfileInterest> interests) {
        return interests.stream()
                .map(interest -> master(interest.getInterest()))
                .toList();
    }

    public List<Map<String, Object>> hobbies(List<ProfileHobby> hobbies) {
        return hobbies.stream()
                .map(hobby -> master(hobby.getHobby()))
                .toList();
    }

    private Map<String, Object> location(UserProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("country", master(profile.getCountry()));
        map.put("state", master(profile.getState()));
        map.put("city", master(profile.getCity()));
        return map;
    }

    private Map<String, Object> verification(ProfileVerification verification) {
        if (verification == null) {
            return Map.of("verificationStatus", "UNVERIFIED");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("verificationStatus", verification.getVerificationStatus());
        map.put("verifiedAt", verification.getVerifiedAt());
        map.put("verificationType", verification.getVerificationType());
        return map;
    }

    private Map<String, Object> master(MasterValue value) {
        if (value == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", value.getId());
        map.put("code", value.getCode());
        map.put("name", value.getName());
        return map;
    }

    private Integer age(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
    }
}
