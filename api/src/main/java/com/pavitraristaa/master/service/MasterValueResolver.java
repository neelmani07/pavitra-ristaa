package com.pavitraristaa.master.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.master.repository.MasterValueRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MasterValueResolver {

    private final MasterValueRepository masterValueRepository;

    public MasterValueResolver(MasterValueRepository masterValueRepository) {
        this.masterValueRepository = masterValueRepository;
    }

    public MasterValue optional(Long id) {
        if (id == null) {
            return null;
        }
        return require(id);
    }

    public MasterValue require(Long id) {
        List<MasterValue> found = masterValueRepository.findByIdInAndActiveTrue(List.of(id));
        if (found.isEmpty()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Master value not found", Map.of("id", id));
        }
        return found.getFirst();
    }

    public List<MasterValue> requireAll(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(ids));
        Map<Long, MasterValue> byId = masterValueRepository.findByIdInAndActiveTrue(distinct).stream()
                .collect(Collectors.toMap(MasterValue::getId, Function.identity()));
        if (byId.size() != distinct.size()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "One or more master values are invalid");
        }
        return distinct.stream().map(byId::get).toList();
    }

    /**
     * Same as {@link #optional(Long)}, but also rejects a value from the wrong category - e.g. an id that
     * resolves to a religion where an education level is expected. The plain lookups only check {@code active},
     * and there is no database constraint tying a column like {@code education_level_id} to one category, so
     * this check is the only thing stopping a value from the wrong list being saved silently.
     */
    public MasterValue optionalInCategory(Long id, String categoryCode) {
        if (id == null) {
            return null;
        }
        return requireInCategory(id, categoryCode);
    }

    public MasterValue requireInCategory(Long id, String categoryCode) {
        MasterValue value = require(id);
        assertCategory(value, categoryCode);
        return value;
    }

    public List<MasterValue> requireAllInCategory(Collection<Long> ids, String categoryCode) {
        List<MasterValue> values = requireAll(ids);
        values.forEach(value -> assertCategory(value, categoryCode));
        return values;
    }

    private void assertCategory(MasterValue value, String categoryCode) {
        if (!value.getCategory().getCode().equals(categoryCode)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Master value does not belong to the " + categoryCode + " category",
                    Map.of("id", value.getId(), "expectedCategory", categoryCode)
            );
        }
    }
}
