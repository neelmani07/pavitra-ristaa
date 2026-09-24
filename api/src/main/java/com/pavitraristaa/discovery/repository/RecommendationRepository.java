package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.discovery.entity.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    @EntityGraph(attributePaths = {"recommendedProfile", "recommendedProfile.user"})
    Page<Recommendation> findByUserAndStatusOrderByScoreDescGeneratedAtDesc(UserAccount user, String status, Pageable pageable);

    void deleteByUser(UserAccount user);
}
