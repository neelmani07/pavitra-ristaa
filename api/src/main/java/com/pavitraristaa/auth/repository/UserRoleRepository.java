package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.entity.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("select ur from UserRole ur join fetch ur.role where ur.user = :user")
    List<UserRole> findByUserWithRole(UserAccount user);
}
