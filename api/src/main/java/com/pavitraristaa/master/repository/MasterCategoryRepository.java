package com.pavitraristaa.master.repository;

import com.pavitraristaa.master.entity.MasterCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterCategoryRepository extends JpaRepository<MasterCategory, Long> {

    List<MasterCategory> findByActiveTrueOrderByNameAsc();

    Optional<MasterCategory> findByCodeIgnoreCaseAndActiveTrue(String code);
}
