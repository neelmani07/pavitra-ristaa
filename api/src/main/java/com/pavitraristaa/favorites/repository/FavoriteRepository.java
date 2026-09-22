package com.pavitraristaa.favorites.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.favorites.entity.Favorite;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // No *-to-many here (profile.photos is batch-loaded, not joined) - see UserProfile.photos for why: an
    // eager-joined collection combined with pagination generates invalid SQL under globally_quoted_identifiers.
    @EntityGraph(attributePaths = {"profile", "profile.user", "profile.country", "profile.state", "profile.city",
            "profile.verification"})
    Page<Favorite> findByUserOrderByCreatedAtDesc(UserAccount user, Pageable pageable);

    Optional<Favorite> findByUserAndProfile(UserAccount user, UserProfile profile);

    boolean existsByUserAndProfile(UserAccount user, UserProfile profile);
}
