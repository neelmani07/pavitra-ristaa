package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.subscriptions.dto.PlanResponse;
import com.pavitraristaa.subscriptions.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Subscription & Payments")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "List active subscription plans")
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.ok(planService.list(), "Plans");
    }

    @GetMapping("/{planCode}")
    @Operation(summary = "Get plan details")
    public ApiResponse<PlanResponse> getOne(@PathVariable String planCode) {
        return ApiResponse.ok(planService.getOne(planCode), "Plan");
    }
}
