package com.pavitraristaa.subscriptions.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"subscription"})
    Optional<Payment> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"subscription"})
    Page<Payment> findByUserOrderByCreatedAtDesc(UserAccount user, Pageable pageable);
}
