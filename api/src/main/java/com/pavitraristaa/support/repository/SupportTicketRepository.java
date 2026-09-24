package com.pavitraristaa.support.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.support.entity.SupportTicket;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"user", "assignedTo"})
    Optional<SupportTicket> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"user", "assignedTo"})
    Page<SupportTicket> findByUserOrderByCreatedAtDesc(UserAccount user, Pageable pageable);
}
