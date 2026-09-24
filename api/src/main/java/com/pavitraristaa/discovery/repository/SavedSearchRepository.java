package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.discovery.entity.SavedSearch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByUserOrderByCreatedAtDesc(UserAccount user);

    Optional<SavedSearch> findByIdAndUser(Long id, UserAccount user);

    List<SavedSearch> findByUserAndIsDefaultTrue(UserAccount user);
}
