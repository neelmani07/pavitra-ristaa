package com.pavitraristaa.preference.repository;

import com.pavitraristaa.preference.entity.PartnerPreference;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerPreferenceRepository extends JpaRepository<PartnerPreference, Long> {

    @EntityGraph(attributePaths = {"preferredMaritalStatus", "values", "values.masterValue"})
    Optional<PartnerPreference> findByProfile(UserProfile profile);
}
