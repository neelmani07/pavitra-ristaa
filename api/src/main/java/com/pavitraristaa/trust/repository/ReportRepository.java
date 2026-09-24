package com.pavitraristaa.trust.repository;

import com.pavitraristaa.trust.entity.Report;
import com.pavitraristaa.trust.entity.ReportStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter", "reportedUser", "reason"})
    Optional<Report> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"reporter", "reportedUser", "reason"})
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "reportedUser", "reason"})
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
