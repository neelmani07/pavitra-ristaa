package com.pavitraristaa.trust.repository;

import com.pavitraristaa.trust.entity.ReportReason;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportReasonRepository extends JpaRepository<ReportReason, Long> {

    List<ReportReason> findByActiveTrueOrderByNameAsc();

    Optional<ReportReason> findByIdAndActiveTrue(Long id);
}
