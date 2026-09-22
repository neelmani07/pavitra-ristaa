package com.pavitraristaa.master.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.master.dto.MasterCategoryResponse;
import com.pavitraristaa.master.dto.MasterValueResponse;
import com.pavitraristaa.master.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/master-data")
@Tag(name = "Master Data")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/categories")
    @Operation(summary = "List master-data categories")
    public ApiResponse<List<MasterCategoryResponse>> categories() {
        return ApiResponse.ok(masterDataService.categories(), "Master categories");
    }

    @GetMapping("/{categoryCode}")
    @Operation(summary = "List active values for a master-data category")
    public ApiResponse<PagedData<MasterValueResponse>> values(
            @PathVariable String categoryCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                masterDataService.values(categoryCode, search, PaginationSupport.pageable(page, size)),
                "Master values"
        );
    }
}
