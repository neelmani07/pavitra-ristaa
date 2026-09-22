package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUuid(UUID uuid);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByMobile(String mobile);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}
