package com.pavitraristaa.subscriptions.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Subscription;
import com.pavitraristaa.subscriptions.entity.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"plan", "user"})
    Optional<Subscription> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"plan", "user"})
    Optional<Subscription> findFirstByUserOrderByCreatedAtDesc(UserAccount user);

    boolean existsByUserAndStatusIn(UserAccount user, java.util.Collection<SubscriptionStatus> statuses);
}
