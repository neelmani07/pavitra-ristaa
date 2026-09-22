package com.pavitraristaa.connections.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.entity.MatchStatus;
import com.pavitraristaa.relationship.entity.RelationshipMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @EntityGraph(attributePaths = {"userA", "userB", "relationshipMode"})
    Optional<Match> findByUuid(UUID uuid);

    Optional<Match> findByUserAAndUserBAndRelationshipMode(UserAccount userA, UserAccount userB, RelationshipMode relationshipMode);

    @EntityGraph(attributePaths = {"userA", "userB", "relationshipMode"})
    @Query("select m from Match m where (m.userA = :user or m.userB = :user) and m.status = :status order by m.matchedAt desc")
    Page<Match> findByUserAndStatus(@Param("user") UserAccount user, @Param("status") MatchStatus status, Pageable pageable);
}
