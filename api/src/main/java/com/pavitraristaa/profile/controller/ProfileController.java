package com.pavitraristaa.profile.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.profile.dto.CreatePhotoRequest;
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
import com.pavitraristaa.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserAccessor currentUserAccessor;

    public ProfileController(ProfileService profileService, CurrentUserAccessor currentUserAccessor) {
        this.profileService = profileService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get my complete profile")
    public ApiResponse<ProfileResponse> getMine() {
        return ApiResponse.ok(profileService.getMine(currentUserAccessor.requireUser()), "Profile");
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update core profile information")
    public ApiResponse<ProfileResponse> updateCore(@RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(profileService.updateCore(currentUserAccessor.requireUser(), request), "Profile updated");
    }

    @GetMapping("/me/profile/education")
    @Operation(summary = "Get education details")
    public ApiResponse<Map<String, Object>> getEducation() {
        return ApiResponse.ok(profileService.getEducation(currentUserAccessor.requireUser()), "Education");
    }

    @PutMapping("/me/profile/education")
    @Operation(summary = "Update education details")
    public ApiResponse<Map<String, Object>> updateEducation(@RequestBody UpdateEducationRequest request) {
        return ApiResponse.ok(profileService.updateEducation(currentUserAccessor.requireUser(), request), "Education updated");
    }

    @GetMapping("/me/profile/career")
    @Operation(summary = "Get career details")
    public ApiResponse<Map<String, Object>> getCareer() {
        return ApiResponse.ok(profileService.getCareer(currentUserAccessor.requireUser()), "Career");
    }

    @PutMapping("/me/profile/career")
    @Operation(summary = "Update career details")
    public ApiResponse<Map<String, Object>> updateCareer(@RequestBody UpdateCareerRequest request) {
        return ApiResponse.ok(profileService.updateCareer(currentUserAccessor.requireUser(), request), "Career updated");
    }

    @GetMapping("/me/profile/family")
    @Operation(summary = "Get family details")
    public ApiResponse<Map<String, Object>> getFamily() {
        return ApiResponse.ok(profileService.getFamily(currentUserAccessor.requireUser()), "Family");
    }

    @PutMapping("/me/profile/family")
    @Operation(summary = "Update family details")
    public ApiResponse<Map<String, Object>> updateFamily(@RequestBody UpdateFamilyRequest request) {
        return ApiResponse.ok(profileService.updateFamily(currentUserAccessor.requireUser(), request), "Family updated");
    }

    @GetMapping("/me/profile/lifestyle")
    @Operation(summary = "Get lifestyle details")
    public ApiResponse<Map<String, Object>> getLifestyle() {
        return ApiResponse.ok(profileService.getLifestyle(currentUserAccessor.requireUser()), "Lifestyle");
    }

    @PutMapping("/me/profile/lifestyle")
    @Operation(summary = "Update lifestyle details")
    public ApiResponse<Map<String, Object>> updateLifestyle(@RequestBody UpdateLifestyleRequest request) {
        return ApiResponse.ok(profileService.updateLifestyle(currentUserAccessor.requireUser(), request), "Lifestyle updated");
    }

    @GetMapping("/me/profile/spiritual")
    @Operation(summary = "Get my spiritual profile")
    public ApiResponse<SpiritualProfileResponse> getSpiritual() {
        return ApiResponse.ok(profileService.getSpiritual(currentUserAccessor.requireUser()), "Spiritual profile");
    }

    @PutMapping("/me/profile/spiritual")
    @Operation(summary = "Replace my spiritual profile")
    public ApiResponse<SpiritualProfileResponse> updateSpiritual(@RequestBody UpdateSpiritualProfileRequest request) {
        return ApiResponse.ok(profileService.updateSpiritual(currentUserAccessor.requireUser(), request), "Spiritual profile updated");
    }

    @GetMapping("/me/profile/languages")
    @Operation(summary = "List profile languages")
    public ApiResponse<List<Map<String, Object>>> getLanguages() {
        return ApiResponse.ok(profileService.getLanguages(currentUserAccessor.requireUser()), "Languages");
    }

    @PutMapping("/me/profile/languages")
    @Operation(summary = "Replace profile languages")
    public ApiResponse<List<Map<String, Object>>> replaceLanguages(@RequestBody ReplaceLanguagesRequest request) {
        return ApiResponse.ok(profileService.replaceLanguages(currentUserAccessor.requireUser(), request), "Languages updated");
    }

    @GetMapping("/me/profile/interests")
    @Operation(summary = "List profile interests")
    public ApiResponse<List<Map<String, Object>>> getInterests() {
        return ApiResponse.ok(profileService.getInterests(currentUserAccessor.requireUser()), "Interests");
    }

    @PutMapping("/me/profile/interests")
    @Operation(summary = "Replace profile interests")
    public ApiResponse<List<Map<String, Object>>> replaceInterests(@RequestBody ReplaceInterestsRequest request) {
        return ApiResponse.ok(profileService.replaceInterests(currentUserAccessor.requireUser(), request), "Interests updated");
    }

    @GetMapping("/me/profile/hobbies")
    @Operation(summary = "List profile hobbies")
    public ApiResponse<List<Map<String, Object>>> getHobbies() {
        return ApiResponse.ok(profileService.getHobbies(currentUserAccessor.requireUser()), "Hobbies");
    }

    @PutMapping("/me/profile/hobbies")
    @Operation(summary = "Replace profile hobbies")
    public ApiResponse<List<Map<String, Object>>> replaceHobbies(@RequestBody ReplaceHobbiesRequest request) {
        return ApiResponse.ok(profileService.replaceHobbies(currentUserAccessor.requireUser(), request), "Hobbies updated");
    }

    @GetMapping("/me/profile/photos")
    @Operation(summary = "List my profile photos")
    public ApiResponse<List<ProfilePhotoResponse>> listPhotos() {
        return ApiResponse.ok(profileService.listPhotos(currentUserAccessor.requireUser()), "Photos");
    }

    @PostMapping("/me/profile/photos")
    @Operation(summary = "Create profile photo metadata after upload")
    public ApiResponse<ProfilePhotoResponse> addPhoto(@Valid @RequestBody CreatePhotoRequest request) {
        return ApiResponse.ok(profileService.addPhoto(currentUserAccessor.requireUser(), request), "Photo added");
    }

    @PutMapping("/me/profile/photos/{photoId}")
    @Operation(summary = "Update profile photo metadata")
    public ApiResponse<ProfilePhotoResponse> updatePhoto(
            @PathVariable UUID photoId,
            @RequestBody UpdatePhotoRequest request
    ) {
        return ApiResponse.ok(profileService.updatePhoto(currentUserAccessor.requireUser(), photoId, request), "Photo updated");
    }

    @DeleteMapping("/me/profile/photos/{photoId}")
    @Operation(summary = "Delete a profile photo")
    public ApiResponse<Void> deletePhoto(@PathVariable UUID photoId) {
        profileService.deletePhoto(currentUserAccessor.requireUser(), photoId);
        return ApiResponse.ok("Photo deleted");
    }

    @PostMapping("/me/profile/publish")
    @Operation(summary = "Publish profile")
    public ApiResponse<ProfileResponse> publish() {
        return ApiResponse.ok(profileService.publish(currentUserAccessor.requireUser()), "Profile published");
    }

    @PostMapping("/me/profile/unpublish")
    @Operation(summary = "Unpublish profile")
    public ApiResponse<ProfileResponse> unpublish() {
        return ApiResponse.ok(profileService.unpublish(currentUserAccessor.requireUser()), "Profile unpublished");
    }

    @GetMapping("/me/profile/preview")
    @Operation(summary = "Preview public profile representation")
    public ApiResponse<ProfileResponse> preview() {
        return ApiResponse.ok(profileService.preview(currentUserAccessor.requireUser()), "Profile preview");
    }

    @GetMapping("/me/profile/completion")
    @Operation(summary = "Get profile completion information")
    public ApiResponse<ProfileCompletionResponse> completion() {
        return ApiResponse.ok(profileService.completion(currentUserAccessor.requireUser()), "Profile completion");
    }

    @GetMapping("/profiles/{profileId}")
    @Operation(summary = "Get another member profile")
    public ApiResponse<ProfileResponse> getPublic(@PathVariable UUID profileId) {
        return ApiResponse.ok(profileService.getPublic(currentUserAccessor.requireUser(), profileId), "Profile");
    }
}
