package com.pavitraristaa.trust.repository;

import com.pavitraristaa.trust.entity.Appeal;
import com.pavitraristaa.trust.entity.AppealStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

    @EntityGraph(attributePaths = {"user", "resolvedBy"})
    Optional<Appeal> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"user", "resolvedBy"})
    Page<Appeal> findByStatusOrderByCreatedAtDesc(AppealStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "resolvedBy"})
    Page<Appeal> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
