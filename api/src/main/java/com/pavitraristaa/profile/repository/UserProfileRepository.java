package com.pavitraristaa.profile.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "country",
            "state",
            "city",
            "education",
            "education.educationLevel",
            "career",
            "career.occupation",
            "career.industry",
            "career.workLocationCity",
            "family",
            "lifestyle",
            "lifestyle.diet",
            "lifestyle.smoking",
            "lifestyle.drinking",
            "lifestyle.exerciseFrequency",
            "spiritualProfile",
            "verification"
    })
    Optional<UserProfile> findByUserAndDeletedFalse(UserAccount user);

    @EntityGraph(attributePaths = {
            "user",
            "country",
            "state",
            "city",
            "education",
            "education.educationLevel",
            "career",
            "career.occupation",
            "career.industry",
            "career.workLocationCity",
            "family",
            "lifestyle",
            "lifestyle.diet",
            "lifestyle.smoking",
            "lifestyle.drinking",
            "lifestyle.exerciseFrequency",
            "spiritualProfile",
            "verification"
    })
    Optional<UserProfile> findByUser_UuidAndDeletedFalse(UUID userUuid);
}
