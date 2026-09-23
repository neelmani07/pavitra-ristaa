package com.pavitraristaa.trust.repository;

import com.pavitraristaa.trust.entity.Report;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter", "reportedUser", "reason"})
    Optional<Report> findByUuid(UUID uuid);
}
