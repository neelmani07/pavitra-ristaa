package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.profile.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

/**
 * The browse/search query, built with DiscoverySpecifications. Lives in the discovery package (not alongside
 * UserProfile in the profile package) so that only discovery, not the core profile module, needs to know about
 * relationship modes and blocks.
 */
public interface DiscoveryProfileRepository extends JpaRepository<UserProfile, Long>, JpaSpecificationExecutor<UserProfile> {

    // No *-to-many in this graph (photos is batch-loaded, not joined here) - see UserProfile.photos for why.
    @EntityGraph(attributePaths = {"user", "country", "state", "city", "verification"})
    Page<UserProfile> findAll(Specification<UserProfile> spec, Pageable pageable);
}
