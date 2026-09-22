package com.pavitraristaa.profile.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // Only one *-to-many path (photos.*) is eager-joined here. UserProfile also has languages/interests/hobbies,
    // each mapped as a plain List (an unordered Hibernate "bag"); joining more than one bag association in the
    // same query throws MultipleBagFetchException, so those three stay lazy rather than risk that at runtime.
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
            "verification",
            "photos",
            "photos.mediaFile"
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
            "verification",
            "photos",
            "photos.mediaFile"
    })
    Optional<UserProfile> findByUser_UuidAndDeletedFalse(UUID userUuid);
}
