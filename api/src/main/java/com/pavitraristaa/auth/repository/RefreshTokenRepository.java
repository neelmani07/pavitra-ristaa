package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.RefreshToken;
import com.pavitraristaa.auth.entity.UserAccount;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Page<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(
            UserAccount user,
            Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :revokedAt where rt.user = :user and rt.revokedAt is null")
    int revokeAllActive(@Param("user") UserAccount user, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revokedAt = :revokedAt
            where rt.user = :user
              and rt.revokedAt is null
              and rt.id <> :keepId
            """)
    int revokeAllActiveExcept(
            @Param("user") UserAccount user,
            @Param("keepId") Long keepId,
            @Param("revokedAt") Instant revokedAt
    );
}
