package com.pavitraristaa.discovery.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.discovery.dto.SearchHistoryResponse;
import com.pavitraristaa.discovery.entity.SearchHistory;
import com.pavitraristaa.discovery.repository.SearchHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SearchHistoryService {

    private final AuthService authService;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ObjectMapper objectMapper;

    public SearchHistoryService(AuthService authService, SearchHistoryRepository searchHistoryRepository, ObjectMapper objectMapper) {
        this.authService = authService;
        this.searchHistoryRepository = searchHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /** Called by DiscoveryService.search() itself - explicit filtered searches are logged, plain browsing isn't. */
    @Transactional
    public void record(UserAccount self, Object criteria) {
        SearchHistory entry = new SearchHistory();
        entry.setUser(self);
        entry.setCriteria(writeCriteria(criteria));
        entry.setSearchedAt(Instant.now());
        searchHistoryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> list(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return searchHistoryRepository.findByUserOrderBySearchedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional
    public void clear(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        searchHistoryRepository.deleteByUser(self);
    }

    private String writeCriteria(Object criteria) {
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

    private SearchHistoryResponse toResponse(SearchHistory entry) {
        return new SearchHistoryResponse(entry.getId(), readCriteria(entry.getCriteria()), entry.getSearchedAt());
    }
}
