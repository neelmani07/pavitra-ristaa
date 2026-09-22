package com.pavitraristaa.media.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.entity.MediaStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByUuidAndOwnerAndStatus(UUID uuid, UserAccount owner, MediaStatus status);

    Optional<MediaFile> findByUuidAndOwnerAndStatusNot(UUID uuid, UserAccount owner, MediaStatus status);
}
