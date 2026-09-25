package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.subscriptions.dto.PlanResponse;
import com.pavitraristaa.subscriptions.entity.Plan;
import com.pavitraristaa.subscriptions.repository.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final ObjectMapper objectMapper;

    public PlanService(PlanRepository planRepository, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        return planRepository.findByActiveTrueOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse getOne(String code) {
        return toResponse(requirePlan(code));
    }

    /** For CouponController, which needs a plan's price without touching the Plan entity itself (controllers
     *  must not depend on ..entity.. classes - see ModuleBoundariesTest.controllersDoNotExposeEntities). */
    @Transactional(readOnly = true)
    public BigDecimal priceOf(String planCode) {
        return requirePlan(planCode).getPrice();
    }

    Plan requirePlan(String code) {
        return planRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new ApiException(ErrorCode.PLAN_NOT_FOUND, "Plan not found"));
    }

    @SuppressWarnings("unchecked")
    PlanResponse toResponse(Plan plan) {
        Map<String, Object> features = plan.getFeatures() == null || plan.getFeatures().isBlank()
                ? Map.of()
                : objectMapper.readValue(plan.getFeatures(), Map.class);
        return new PlanResponse(
                plan.getCode(), plan.getName(), plan.getDescription(), plan.getPrice(), plan.getCurrencyCode(),
                plan.getBillingPeriod().name(), plan.getDurationDays(), features);
    }
}
