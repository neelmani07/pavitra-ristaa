package com.pavitraristaa.relationship.repository;

import com.pavitraristaa.relationship.entity.RelationshipMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationshipModeRepository extends JpaRepository<RelationshipMode, Long> {

    List<RelationshipMode> findByActiveTrueOrderByDisplayOrderAsc();

    List<RelationshipMode> findByCodeInAndActiveTrue(Collection<String> codes);

    Optional<RelationshipMode> findByCodeAndActiveTrue(String code);
}
