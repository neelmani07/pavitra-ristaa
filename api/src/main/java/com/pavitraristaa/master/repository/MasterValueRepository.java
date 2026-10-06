package com.pavitraristaa.master.repository;

import com.pavitraristaa.master.entity.MasterValue;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Includes inactive rows on purpose: a value a user typed in that is still awaiting review must be reused, not duplicated. */
    List<MasterValue> findByCategory_CodeIgnoreCaseAndNameIgnoreCase(String categoryCode, String name);

    List<MasterValue> findByCategory_CodeIgnoreCaseAndCodeIgnoreCase(String categoryCode, String code);

    /** Every active value of every active category, grouped by category in the order a client wants to show them. */
    List<MasterValue> findByActiveTrueAndCategory_ActiveTrueOrderByCategory_CodeAscDisplayOrderAscNameAsc();

    /**
     * Race-safe create: two requests creating the same new value at once both succeed, and the loser simply reads
     * back the row the winner inserted instead of failing on the (category_id, code) unique constraint.
     */
    @Modifying
    @Query(value = "INSERT INTO master_value (category_id, code, name, display_order, is_active, metadata) "
            + "VALUES (:categoryId, :code, :name, 0, :active, CAST(:metadata AS jsonb)) "
            + "ON CONFLICT (category_id, code) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(
            @Param("categoryId") Long categoryId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("active") boolean active,
            @Param("metadata") String metadata
    );
}
