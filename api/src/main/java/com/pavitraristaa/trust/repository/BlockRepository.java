package com.pavitraristaa.trust.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.trust.entity.Block;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, Long> {

    @Query("""
            select count(b) > 0 from Block b
            where (b.blocker = :userA and b.blocked = :userB)
               or (b.blocker = :userB and b.blocked = :userA)
            """)
    boolean existsEitherDirection(@Param("userA") UserAccount userA, @Param("userB") UserAccount userB);

    Optional<Block> findByBlockerAndBlocked(UserAccount blocker, UserAccount blocked);

    @EntityGraph(attributePaths = {"blocked"})
    Page<Block> findByBlockerOrderByCreatedAtDesc(UserAccount blocker, Pageable pageable);
}
