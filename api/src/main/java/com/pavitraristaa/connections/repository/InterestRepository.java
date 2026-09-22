package com.pavitraristaa.connections.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.connections.entity.Interest;
import com.pavitraristaa.connections.entity.InterestStatus;
import com.pavitraristaa.relationship.entity.RelationshipMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    @EntityGraph(attributePaths = {"sender", "receiver", "relationshipMode"})
    Optional<Interest> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"sender", "receiver", "relationshipMode"})
    Page<Interest> findBySenderOrderByCreatedAtDesc(UserAccount sender, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver", "relationshipMode"})
    Page<Interest> findByReceiverOrderByCreatedAtDesc(UserAccount receiver, Pageable pageable);

    boolean existsBySenderAndReceiverAndRelationshipModeAndStatus(
            UserAccount sender, UserAccount receiver, RelationshipMode relationshipMode, InterestStatus status);
}
