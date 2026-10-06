package com.pavitraristaa.master.service;

import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.master.dto.MasterCategoryResponse;
import com.pavitraristaa.master.dto.MasterValueResponse;
import com.pavitraristaa.master.entity.MasterCategory;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.master.repository.MasterCategoryRepository;
import com.pavitraristaa.master.repository.MasterValueRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataService {

    private final MasterCategoryRepository masterCategoryRepository;
    private final MasterValueRepository masterValueRepository;

    public MasterDataService(
            MasterCategoryRepository masterCategoryRepository,
            MasterValueRepository masterValueRepository
    ) {
        this.masterCategoryRepository = masterCategoryRepository;
        this.masterValueRepository = masterValueRepository;
    }

    @Transactional(readOnly = true)
    public List<MasterCategoryResponse> categories() {
        return masterCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> new MasterCategoryResponse(category.getCode(), category.getName(), category.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedData<MasterValueResponse> values(String categoryCode, String search, Pageable pageable) {
        MasterCategory category = masterCategoryRepository.findByCodeIgnoreCaseAndActiveTrue(categoryCode)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Master category not found"));
        Page<MasterValue> page = search == null || search.isBlank()
                ? masterValueRepository.findByCategory_CodeIgnoreCaseAndActiveTrueOrderByDisplayOrderAsc(
                        category.getCode(), pageable)
                : masterValueRepository.findByCategory_CodeIgnoreCaseAndActiveTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAsc(
                        category.getCode(), search.trim(), pageable);
        return new PagedData<>(
                page.getContent().stream()
                        .map(value -> new MasterValueResponse(
                                value.getId(),
                                value.getCode(),
                                value.getName(),
                                value.getDisplayOrder()
                        ))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /** Every active value of every active category, keyed by category code, minus the categories listed in {@code exclude}. */
    @Transactional(readOnly = true)
    public Map<String, List<MasterValueResponse>> allValuesByCategory(Collection<String> exclude) {
        Map<String, List<MasterValueResponse>> byCategory = new LinkedHashMap<>();
        for (MasterValue value : masterValueRepository.findByActiveTrueAndCategory_ActiveTrueOrderByCategory_CodeAscDisplayOrderAscNameAsc()) {
            String category = value.getCategory().getCode();
            if (exclude.contains(category)) {
                continue;
            }
            byCategory.computeIfAbsent(category, key -> new java.util.ArrayList<>())
                    .add(new MasterValueResponse(value.getId(), value.getCode(), value.getName(), value.getDisplayOrder()));
        }
        return byCategory;
    }
}
