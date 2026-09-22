package com.pavitraristaa.profile.service;

import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.service.MediaUsageChecker;
import com.pavitraristaa.profile.repository.ProfilePhotoRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfilePhotoMediaUsage implements MediaUsageChecker {

    private final ProfilePhotoRepository profilePhotoRepository;

    public ProfilePhotoMediaUsage(ProfilePhotoRepository profilePhotoRepository) {
        this.profilePhotoRepository = profilePhotoRepository;
    }

    @Override
    public boolean isInUse(MediaFile media) {
        return profilePhotoRepository.existsByMediaFile(media);
    }
}
