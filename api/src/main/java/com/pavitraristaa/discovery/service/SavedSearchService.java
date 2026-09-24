package com.pavitraristaa.discovery.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.discovery.dto.CreateSavedSearchRequest;
import com.pavitraristaa.discovery.dto.SavedSearchResponse;
import com.pavitraristaa.discovery.dto.UpdateSavedSearchRequest;
import com.pavitraristaa.discovery.entity.SavedSearch;
import com.pavitraristaa.discovery.repository.SavedSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SavedSearchService {

    private final AuthService authService;
    private final SavedSearchRepository savedSearchRepository;
    private final ObjectMapper objectMapper;

    public SavedSearchService(AuthService authService, SavedSearchRepository savedSearchRepository, ObjectMapper objectMapper) {
        this.authService = authService;
        this.savedSearchRepository = savedSearchRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponse> list(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        return savedSearchRepository.findByUserOrderByCreatedAtDesc(self).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SavedSearchResponse create(AuthenticatedUser principal, CreateSavedSearchRequest request) {
        UserAccount self = authService.requireUsable(principal);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearExistingDefault(self);
        }
        Instant now = Instant.now();
        SavedSearch search = new SavedSearch();
        search.setUser(self);
        search.setName(request.name().trim());
        search.setCriteria(writeCriteria(request.criteria()));
        search.setDefault(makeDefault);
        search.setCreatedAt(now);
        search.setUpdatedAt(now);
        return toResponse(savedSearchRepository.save(search));
    }

    @Transactional
    public SavedSearchResponse update(AuthenticatedUser principal, Long searchId, UpdateSavedSearchRequest request) {
        UserAccount self = authService.requireUsable(principal);
        SavedSearch search = requireOwned(self, searchId);
        if (request.name() != null && !request.name().isBlank()) {
            search.setName(request.name().trim());
        }
        if (request.criteria() != null) {
            search.setCriteria(writeCriteria(request.criteria()));
        }
        if (request.isDefault() != null) {
            if (request.isDefault()) {
                clearExistingDefault(self);
            }
            search.setDefault(request.isDefault());
        }
        search.setUpdatedAt(Instant.now());
        return toResponse(savedSearchRepository.save(search));
    }

    @Transactional
    public void delete(AuthenticatedUser principal, Long searchId) {
        UserAccount self = authService.requireUsable(principal);
        savedSearchRepository.delete(requireOwned(self, searchId));
    }

    private void clearExistingDefault(UserAccount self) {
        for (SavedSearch existing : savedSearchRepository.findByUserAndIsDefaultTrue(self)) {
            existing.setDefault(false);
            savedSearchRepository.save(existing);
        }
    }

    private SavedSearch requireOwned(UserAccount self, Long searchId) {
        return savedSearchRepository.findByIdAndUser(searchId, self)
                .orElseThrow(() -> new ApiException(ErrorCode.SAVED_SEARCH_NOT_FOUND, "Saved search not found"));
    }

    private String writeCriteria(Map<String, Object> criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid criteria");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCriteria(String criteria) {
        return objectMapper.readValue(criteria, Map.class);
    }

    private SavedSearchResponse toResponse(SavedSearch search) {
        return new SavedSearchResponse(
                search.getId(), search.getName(), readCriteria(search.getCriteria()), search.isDefault(),
                search.getCreatedAt(), search.getUpdatedAt());
    }
}
