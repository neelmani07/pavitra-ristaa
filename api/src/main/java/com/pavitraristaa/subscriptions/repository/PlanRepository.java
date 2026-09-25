package com.pavitraristaa.subscriptions.repository;

import com.pavitraristaa.subscriptions.entity.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<Plan> findByCodeIgnoreCaseAndActiveTrue(String code);
}
