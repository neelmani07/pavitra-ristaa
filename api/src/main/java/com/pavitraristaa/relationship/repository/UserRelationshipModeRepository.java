package com.pavitraristaa.relationship.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.relationship.entity.UserRelationshipMode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRelationshipModeRepository extends JpaRepository<UserRelationshipMode, Long> {

    @Query("""
            select urm from UserRelationshipMode urm
            join fetch urm.relationshipMode
            where urm.user = :user
            order by urm.relationshipMode.displayOrder
            """)
    List<UserRelationshipMode> findByUser(@Param("user") UserAccount user);

    void deleteByUser(UserAccount user);
}
