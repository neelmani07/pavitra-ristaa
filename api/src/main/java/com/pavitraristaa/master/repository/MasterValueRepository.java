package com.pavitraristaa.master.repository;

import com.pavitraristaa.master.entity.MasterValue;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterValueRepository extends JpaRepository<MasterValue, Long> {

    List<MasterValue> findByIdInAndActiveTrue(Collection<Long> ids);

    Page<MasterValue> findByCategory_CodeIgnoreCaseAndActiveTrueOrderByDisplayOrderAsc(
            String categoryCode,
            Pageable pageable
    );

    Page<MasterValue> findByCategory_CodeIgnoreCaseAndActiveTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAsc(
            String categoryCode,
            String name,
            Pageable pageable
    );
}
