package com.pavitraristaa.profile.repository;

import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.profile.entity.ProfilePhoto;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, Long> {

    List<ProfilePhoto> findByProfileOrderByDisplayOrderAsc(UserProfile profile);

    Optional<ProfilePhoto> findByUuidAndProfile(UUID uuid, UserProfile profile);

    Optional<ProfilePhoto> findByProfileAndPrimaryTrue(UserProfile profile);

    boolean existsByMediaFile(MediaFile mediaFile);
}
