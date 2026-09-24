package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.discovery.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Page<SearchHistory> findByUserOrderBySearchedAtDesc(UserAccount user, Pageable pageable);

    void deleteByUser(UserAccount user);
}
