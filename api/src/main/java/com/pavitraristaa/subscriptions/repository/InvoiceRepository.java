package com.pavitraristaa.subscriptions.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByUuid(UUID uuid);

    Page<Invoice> findByUserOrderByIssuedAtDesc(UserAccount user, Pageable pageable);
}
