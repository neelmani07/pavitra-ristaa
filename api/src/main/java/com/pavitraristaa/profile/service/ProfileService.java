package com.pavitraristaa.profile.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.master.service.MasterValueResolver;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.entity.MediaStatus;
import com.pavitraristaa.media.repository.MediaFileRepository;
import com.pavitraristaa.profile.dto.CreatePhotoRequest;
import com.pavitraristaa.profile.dto.LanguageItemRequest;
import com.pavitraristaa.profile.dto.ProfileCompletionResponse;
import com.pavitraristaa.profile.dto.ProfilePhotoResponse;
import com.pavitraristaa.profile.dto.ProfileResponse;
import com.pavitraristaa.profile.dto.ReplaceHobbiesRequest;
import com.pavitraristaa.profile.dto.ReplaceInterestsRequest;
import com.pavitraristaa.profile.dto.ReplaceLanguagesRequest;
import com.pavitraristaa.profile.dto.SpiritualProfileResponse;
import com.pavitraristaa.profile.dto.UpdateCareerRequest;
import com.pavitraristaa.profile.dto.UpdateEducationRequest;
import com.pavitraristaa.profile.dto.UpdateFamilyRequest;
import com.pavitraristaa.profile.dto.UpdateLifestyleRequest;
import com.pavitraristaa.profile.dto.UpdatePhotoRequest;
import com.pavitraristaa.profile.dto.UpdateProfileRequest;
import com.pavitraristaa.profile.dto.UpdateSpiritualProfileRequest;
import com.pavitraristaa.profile.entity.PhotoApprovalStatus;
import com.pavitraristaa.profile.entity.PhotoType;
import com.pavitraristaa.profile.entity.PhotoVisibility;
import com.pavitraristaa.profile.entity.ProfileCareer;
import com.pavitraristaa.profile.entity.ProfileEducation;
import com.pavitraristaa.profile.entity.ProfileFamily;
import com.pavitraristaa.profile.entity.ProfileHobby;
import com.pavitraristaa.profile.entity.ProfileInterest;
import com.pavitraristaa.profile.entity.ProfileLanguage;
import com.pavitraristaa.profile.entity.ProfileLifestyle;
import com.pavitraristaa.profile.entity.ProfilePhoto;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.SpiritualProfile;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.ProfileMapper;
import com.pavitraristaa.profile.repository.ProfilePhotoRepository;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.relationship.entity.UserRelationshipMode;
import com.pavitraristaa.relationship.repository.UserRelationshipModeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;
    private final ProfilePhotoRepository profilePhotoRepository;
    private final UserRelationshipModeRepository userRelationshipModeRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MasterValueResolver masterValueResolver;
    private final ProfileMapper profileMapper;
    private final PavitraProperties properties;

    public ProfileService(
            AuthService authService,
            UserProfileRepository userProfileRepository,
            ProfilePhotoRepository profilePhotoRepository,
            UserRelationshipModeRepository userRelationshipModeRepository,
            MediaFileRepository mediaFileRepository,
            MasterValueResolver masterValueResolver,
            ProfileMapper profileMapper,
            PavitraProperties properties
    ) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.profilePhotoRepository = profilePhotoRepository;
        this.userRelationshipModeRepository = userRelationshipModeRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.masterValueResolver = masterValueResolver;
        this.profileMapper = profileMapper;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMine(AuthenticatedUser principal) {
        UserAccount user = authService.requireUsable(principal);
        UserProfile profile = requireMine(user);
        return toResponse(profile, true);
    }

    @Transactional
    public ProfileResponse updateCore(AuthenticatedUser principal, UpdateProfileRequest request) {
        UserAccount user = authService.requireUsable(principal);
        UserProfile profile = userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseGet(() -> newProfile(user, request));
        applyCore(profile, request, profile.getId() == null);
        refreshCompletion(profile);
        UserProfile saved = userProfileRepository.save(profile);
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEducation(AuthenticatedUser principal) {
        return profileMapper.education(requireMine(authService.requireUsable(principal)).getEducation());
    }

    @Transactional
    public Map<String, Object> updateEducation(AuthenticatedUser principal, UpdateEducationRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfileEducation education = profile.getEducation();
        if (education == null) {
            education = new ProfileEducation();
            education.setProfile(profile);
            profile.setEducation(education);
        }
        education.setEducationLevel(masterValueResolver.optional(request.educationLevelId()));
        education.setInstitutionName(request.institutionName());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setGraduationYear(request.graduationYear());
        education.setAdditionalDetails(request.additionalDetails());
        refreshCompletion(profile);
        return profileMapper.education(userProfileRepository.save(profile).getEducation());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCareer(AuthenticatedUser principal) {
        return profileMapper.career(requireMine(authService.requireUsable(principal)).getCareer());
    }

    @Transactional
    public Map<String, Object> updateCareer(AuthenticatedUser principal, UpdateCareerRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfileCareer career = profile.getCareer();
        if (career == null) {
            career = new ProfileCareer();
            career.setProfile(profile);
            profile.setCareer(career);
        }
        career.setOccupation(masterValueResolver.optional(request.occupationId()));
        career.setJobTitle(request.jobTitle());
        career.setCompanyName(request.companyName());
        career.setIndustry(masterValueResolver.optional(request.industryId()));
        career.setWorkLocationCity(masterValueResolver.optional(request.workLocationCityId()));
        career.setExperienceYears(request.experienceYears());
        if (request.employed() != null) {
            career.setEmployed(request.employed());
        }
        refreshCompletion(profile);
        return profileMapper.career(userProfileRepository.save(profile).getCareer());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFamily(AuthenticatedUser principal) {
        return profileMapper.family(requireMine(authService.requireUsable(principal)).getFamily());
    }

    @Transactional
    public Map<String, Object> updateFamily(AuthenticatedUser principal, UpdateFamilyRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfileFamily family = profile.getFamily();
        if (family == null) {
            family = new ProfileFamily();
            family.setProfile(profile);
            profile.setFamily(family);
        }
        family.setFamilyType(request.familyType());
        family.setParentsStatus(request.parentsStatus());
        family.setSiblingsCount(request.siblingsCount());
        family.setFamilyDescription(request.familyDescription());
        family.setFamilyValues(request.familyValues());
        refreshCompletion(profile);
        return profileMapper.family(userProfileRepository.save(profile).getFamily());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLifestyle(AuthenticatedUser principal) {
        return profileMapper.lifestyle(requireMine(authService.requireUsable(principal)).getLifestyle());
    }

    @Transactional
    public Map<String, Object> updateLifestyle(AuthenticatedUser principal, UpdateLifestyleRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfileLifestyle lifestyle = profile.getLifestyle();
        if (lifestyle == null) {
            lifestyle = new ProfileLifestyle();
            lifestyle.setProfile(profile);
            profile.setLifestyle(lifestyle);
        }
        lifestyle.setDiet(masterValueResolver.optional(request.dietId()));
        lifestyle.setSmoking(masterValueResolver.optional(request.smokingId()));
        lifestyle.setDrinking(masterValueResolver.optional(request.drinkingId()));
        lifestyle.setExerciseFrequency(masterValueResolver.optional(request.exerciseFrequencyId()));
        lifestyle.setSleepPattern(request.sleepPattern());
        lifestyle.setPets(request.pets());
        refreshCompletion(profile);
        return profileMapper.lifestyle(userProfileRepository.save(profile).getLifestyle());
    }

    @Transactional(readOnly = true)
    public SpiritualProfileResponse getSpiritual(AuthenticatedUser principal) {
        return profileMapper.spiritual(requireMine(authService.requireUsable(principal)).getSpiritualProfile());
    }

    @Transactional
    public SpiritualProfileResponse updateSpiritual(AuthenticatedUser principal, UpdateSpiritualProfileRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        Instant now = Instant.now();
        SpiritualProfile spiritual = profile.getSpiritualProfile();
        if (spiritual == null) {
            spiritual = new SpiritualProfile();
            spiritual.setProfile(profile);
            spiritual.setCreatedAt(now);
            profile.setSpiritualProfile(spiritual);
        }
        spiritual.setSpiritualCommunity(request.spiritualCommunity());
        spiritual.setSpiritualInterests(request.spiritualInterests());
        spiritual.setPractices(request.practices());
        spiritual.setAnySpiritualProfession(request.anySpiritualProfession());
        spiritual.setDreamSpiritualPilgrimageDestination(request.dreamSpiritualPilgrimageDestination());
        spiritual.setUpdatedAt(now);
        return profileMapper.spiritual(userProfileRepository.save(profile).getSpiritualProfile());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLanguages(AuthenticatedUser principal) {
        return profileMapper.languages(requireMine(authService.requireUsable(principal)).getLanguages());
    }

    @Transactional
    public List<Map<String, Object>> replaceLanguages(AuthenticatedUser principal, ReplaceLanguagesRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        Map<Long, LanguageItemRequest> requested = new LinkedHashMap<>();
        for (LanguageItemRequest item : request.languages() == null ? List.<LanguageItemRequest>of() : request.languages()) {
            if (item.languageId() == null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "languageId is required");
            }
            requested.put(item.languageId(), item);
        }
        List<MasterValue> desired = masterValueResolver.requireAll(requested.keySet());
        // Update in place: clear-and-re-add would insert before deleting and violate uq_profile_language.
        syncByMasterValue(profile.getLanguages(), desired, ProfileLanguage::getLanguage, value -> {
            ProfileLanguage language = new ProfileLanguage();
            language.setProfile(profile);
            language.setLanguage(value);
            return language;
        });
        for (ProfileLanguage language : profile.getLanguages()) {
            LanguageItemRequest item = requested.get(language.getLanguage().getId());
            language.setProficiency(item.proficiency());
            language.setPrimary(Boolean.TRUE.equals(item.primary()));
        }
        refreshCompletion(profile);
        return profileMapper.languages(userProfileRepository.save(profile).getLanguages());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInterests(AuthenticatedUser principal) {
        return profileMapper.interests(requireMine(authService.requireUsable(principal)).getInterests());
    }

    @Transactional
    public List<Map<String, Object>> replaceInterests(AuthenticatedUser principal, ReplaceInterestsRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        syncByMasterValue(profile.getInterests(), masterValueResolver.requireAll(request.interestIds()),
                ProfileInterest::getInterest, value -> {
                    ProfileInterest interest = new ProfileInterest();
                    interest.setProfile(profile);
                    interest.setInterest(value);
                    return interest;
                });
        refreshCompletion(profile);
        return profileMapper.interests(userProfileRepository.save(profile).getInterests());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHobbies(AuthenticatedUser principal) {
        return profileMapper.hobbies(requireMine(authService.requireUsable(principal)).getHobbies());
    }

    @Transactional
    public List<Map<String, Object>> replaceHobbies(AuthenticatedUser principal, ReplaceHobbiesRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        syncByMasterValue(profile.getHobbies(), masterValueResolver.requireAll(request.hobbyIds()),
                ProfileHobby::getHobby, value -> {
                    ProfileHobby hobby = new ProfileHobby();
                    hobby.setProfile(profile);
                    hobby.setHobby(value);
                    return hobby;
                });
        refreshCompletion(profile);
        return profileMapper.hobbies(userProfileRepository.save(profile).getHobbies());
    }

    @Transactional(readOnly = true)
    public List<ProfilePhotoResponse> listPhotos(AuthenticatedUser principal) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        return profile.getPhotos().stream()
                .map(profileMapper::toPhoto)
                .toList();
    }

    @Transactional
    public ProfilePhotoResponse addPhoto(AuthenticatedUser principal, CreatePhotoRequest request) {
        UserAccount user = authService.requireUsable(principal);
        UserProfile profile = requireMine(user);
        if (request.mediaFileId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "mediaFileId is required");
        }
        MediaFile media = mediaFileRepository
                .findByUuidAndOwnerAndStatus(request.mediaFileId(), user, MediaStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.MEDIA_NOT_FOUND, "Media file not found"));
        Instant now = Instant.now();
        ProfilePhoto photo = new ProfilePhoto();
        photo.setUuid(UUID.randomUUID());
        photo.setProfile(profile);
        photo.setMediaFile(media);
        photo.setPhotoType(parseEnum(PhotoType.class, request.photoType(), "photoType", PhotoType.ADDITIONAL));
        photo.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder().shortValue());
        photo.setVisibility(parseEnum(PhotoVisibility.class, request.visibility(), "visibility", PhotoVisibility.PUBLIC));
        photo.setApprovalStatus(PhotoApprovalStatus.PENDING);
        photo.setCreatedAt(now);
        photo.setUpdatedAt(now);
        boolean primary = Boolean.TRUE.equals(request.primary()) || profile.getPhotos().isEmpty();
        if (primary) {
            profile.getPhotos().forEach(existing -> existing.setPrimary(false));
        }
        photo.setPrimary(primary);
        profile.getPhotos().add(photo);
        refreshCompletion(profile);
        userProfileRepository.save(profile);
        return profileMapper.toPhoto(photo);
    }

    @Transactional
    public ProfilePhotoResponse updatePhoto(AuthenticatedUser principal, UUID photoId, UpdatePhotoRequest request) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfilePhoto photo = profilePhotoRepository.findByUuidAndProfile(photoId, profile)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Photo not found"));
        if (request.photoType() != null) {
            photo.setPhotoType(parseEnum(PhotoType.class, request.photoType(), "photoType", photo.getPhotoType()));
        }
        if (request.displayOrder() != null) {
            photo.setDisplayOrder(request.displayOrder().shortValue());
        }
        if (request.visibility() != null) {
            photo.setVisibility(parseEnum(PhotoVisibility.class, request.visibility(), "visibility", photo.getVisibility()));
        }
        if (Boolean.TRUE.equals(request.primary())) {
            profile.getPhotos().forEach(existing -> existing.setPrimary(false));
            photo.setPrimary(true);
        } else if (Boolean.FALSE.equals(request.primary())) {
            photo.setPrimary(false);
        }
        photo.setUpdatedAt(Instant.now());
        return profileMapper.toPhoto(profilePhotoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(AuthenticatedUser principal, UUID photoId) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        ProfilePhoto photo = profilePhotoRepository.findByUuidAndProfile(photoId, profile)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Photo not found"));
        profile.getPhotos().remove(photo);
        profilePhotoRepository.delete(photo);
        refreshCompletion(profile);
        userProfileRepository.save(profile);
    }

    @Transactional
    public ProfileResponse publish(AuthenticatedUser principal) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        List<String> missing = missingSections(profile);
        if (!missing.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Profile is incomplete and cannot be published",
                    Map.of("missingSections", missing)
            );
        }
        profile.setProfileStatus(ProfileStatus.ACTIVE);
        profile.setUpdatedAt(Instant.now());
        return toResponse(userProfileRepository.save(profile), true);
    }

    @Transactional
    public ProfileResponse unpublish(AuthenticatedUser principal) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        profile.setProfileStatus(ProfileStatus.HIDDEN);
        profile.setUpdatedAt(Instant.now());
        return toResponse(userProfileRepository.save(profile), true);
    }

    @Transactional(readOnly = true)
    public ProfileResponse preview(AuthenticatedUser principal) {
        return toResponse(requireMine(authService.requireUsable(principal)), false);
    }

    @Transactional(readOnly = true)
    public ProfileCompletionResponse completion(AuthenticatedUser principal) {
        UserProfile profile = requireMine(authService.requireUsable(principal));
        return new ProfileCompletionResponse(profile.getProfileCompletionPercent(), missingSections(profile));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getPublic(AuthenticatedUser principal, UUID profileId) {
        UserAccount viewer = authService.requireUsable(principal);
        UserProfile profile = userProfileRepository.findByUser_UuidAndDeletedFalse(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        boolean owner = profile.getUser().getUuid().equals(viewer.getUuid());
        if (!owner && profile.getProfileStatus() != ProfileStatus.ACTIVE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found");
        }
        return toResponse(profile, owner);
    }

    private UserProfile newProfile(UserAccount user, UpdateProfileRequest request) {
        if (blank(request.firstName()) || request.dateOfBirth() == null || blank(request.gender())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "firstName, dateOfBirth and gender are required");
        }
        Instant now = Instant.now();
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setProfileStatus(ProfileStatus.DRAFT);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setDeleted(false);
        profile.setVersion(0L);
        return profile;
    }

    private void applyCore(UserProfile profile, UpdateProfileRequest request, boolean creating) {
        if (creating || request.firstName() != null) {
            requireText(request.firstName(), "firstName");
            profile.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            profile.setLastName(blank(request.lastName()) ? null : request.lastName().trim());
        }
        if (request.displayName() != null) {
            profile.setDisplayName(blank(request.displayName()) ? null : request.displayName().trim());
        }
        if (creating || request.dateOfBirth() != null) {
            validateAge(request.dateOfBirth());
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (creating || request.gender() != null) {
            requireText(request.gender(), "gender");
            profile.setGender(request.gender().trim());
        }
        if (request.headline() != null) {
            profile.setHeadline(blank(request.headline()) ? null : request.headline().trim());
        }
        if (request.aboutMe() != null) {
            profile.setAboutMe(blank(request.aboutMe()) ? null : request.aboutMe());
        }
        if (request.countryId() != null) {
            profile.setCountry(masterValueResolver.require(request.countryId()));
        }
        if (request.stateId() != null) {
            profile.setState(masterValueResolver.require(request.stateId()));
        }
        if (request.cityId() != null) {
            profile.setCity(masterValueResolver.require(request.cityId()));
        }
        profile.setUpdatedAt(Instant.now());
    }

    private void validateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "dateOfBirth is required");
        }
        int age = Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
        if (age < properties.getAuth().getMinAgeYears()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "User must be at least " + properties.getAuth().getMinAgeYears() + " years old"
            );
        }
    }

    private UserProfile requireMine(UserAccount user) {
        return userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
    }

    private ProfileResponse toResponse(UserProfile profile, boolean includePrivatePhotos) {
        List<String> modes = userRelationshipModeRepository.findByUser(profile.getUser()).stream()
                .map(UserRelationshipMode::getRelationshipMode)
                .map(mode -> mode.getCode())
                .toList();
        return profileMapper.toResponse(profile, modes, includePrivatePhotos);
    }

    private void refreshCompletion(UserProfile profile) {
        List<String> missing = missingSections(profile);
        int total = requiredSections().size();
        int percent = Math.round(((total - missing.size()) * 100f) / total);
        profile.setProfileCompletionPercent((short) percent);
        profile.setUpdatedAt(Instant.now());
    }

    private List<String> missingSections(UserProfile profile) {
        List<String> missing = new ArrayList<>();
        if (blank(profile.getFirstName()) || profile.getDateOfBirth() == null || blank(profile.getGender())) {
            missing.add("BASIC");
        }
        if (profile.getCity() == null && profile.getCountry() == null) {
            missing.add("LOCATION");
        }
        if (profile.getEducation() == null) {
            missing.add("EDUCATION");
        }
        if (profile.getCareer() == null) {
            missing.add("CAREER");
        }
        if (profile.getFamily() == null) {
            missing.add("FAMILY");
        }
        if (profile.getLifestyle() == null) {
            missing.add("LIFESTYLE");
        }
        if (profile.getLanguages().isEmpty()) {
            missing.add("LANGUAGES");
        }
        if (profile.getInterests().isEmpty() && profile.getHobbies().isEmpty()) {
            missing.add("INTERESTS");
        }
        if (profile.getPhotos().isEmpty()) {
            missing.add("PHOTOS");
        }
        if (userRelationshipModeRepository.findByUser(profile.getUser()).isEmpty()) {
            missing.add("RELATIONSHIP_MODES");
        }
        return missing;
    }

    private EnumSet<ProfileSection> requiredSections() {
        return EnumSet.allOf(ProfileSection.class);
    }

    private enum ProfileSection {
        BASIC,
        LOCATION,
        EDUCATION,
        CAREER,
        FAMILY,
        LIFESTYLE,
        LANGUAGES,
        INTERESTS,
        PHOTOS,
        RELATIONSHIP_MODES
    }

    /** Removes rows whose value is no longer wanted and adds only the new ones. */
    private <E> void syncByMasterValue(
            List<E> current,
            List<MasterValue> desired,
            Function<E, MasterValue> valueOf,
            Function<MasterValue, E> create
    ) {
        Set<Long> wanted = new HashSet<>();
        desired.forEach(value -> wanted.add(value.getId()));
        current.removeIf(row -> !wanted.contains(valueOf.apply(row).getId()));
        Set<Long> present = new HashSet<>();
        current.forEach(row -> present.add(valueOf.apply(row).getId()));
        for (MasterValue value : desired) {
            if (!present.contains(value.getId())) {
                current.add(create.apply(value));
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid " + field, Map.of(field, value));
        }
    }

    private void requireText(String value, String field) {
        if (blank(value)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, field + " is required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
