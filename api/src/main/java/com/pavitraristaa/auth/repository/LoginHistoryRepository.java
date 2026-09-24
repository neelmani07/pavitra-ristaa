package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.LoginHistory;
import com.pavitraristaa.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserOrderByLoginAtDesc(UserAccount user, Pageable pageable);
}
