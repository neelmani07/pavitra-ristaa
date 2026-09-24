package com.pavitraristaa.admin.repository;

import com.pavitraristaa.admin.entity.Moderation;
import com.pavitraristaa.admin.entity.ModerationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationRepository extends JpaRepository<Moderation, Long> {

    @EntityGraph(attributePaths = {"targetUser", "moderator"})
    Optional<Moderation> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"targetUser", "moderator"})
    Page<Moderation> findByStatusOrderByCreatedAtDesc(ModerationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"targetUser", "moderator"})
    Page<Moderation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
