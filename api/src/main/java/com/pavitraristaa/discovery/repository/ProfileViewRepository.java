package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.discovery.entity.ProfileView;
import com.pavitraristaa.profile.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    // One row per distinct profile, keyed by that viewer's most recent view of it. No *-to-many in this graph
    // (viewedProfile.photos is batch-loaded, not joined) - see UserProfile.photos for why.
    @EntityGraph(attributePaths = {"viewedProfile", "viewedProfile.user", "viewedProfile.country",
            "viewedProfile.state", "viewedProfile.city", "viewedProfile.verification"})
    @Query("""
            select v from ProfileView v
            where v.viewer = :viewer
              and v.viewedAt = (
                  select max(v2.viewedAt) from ProfileView v2
                  where v2.viewer = :viewer and v2.viewedProfile = v.viewedProfile
              )
            order by v.viewedAt desc
            """)
    Page<ProfileView> findMostRecentDistinctByViewer(@Param("viewer") UserAccount viewer, Pageable pageable);

    boolean existsByViewerAndViewedProfile(UserAccount viewer, UserProfile viewedProfile);
}
