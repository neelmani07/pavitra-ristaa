package com.pavitraristaa.discovery.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.discovery.dto.CollectionDetailResponse;
import com.pavitraristaa.discovery.dto.CollectionSummaryResponse;
import com.pavitraristaa.discovery.dto.DiscoverySearchRequest;
import com.pavitraristaa.discovery.entity.DiscoveryCollection;
import com.pavitraristaa.discovery.repository.DiscoveryCollectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class DiscoveryCollectionService {

    private static final DiscoverySearchRequest EMPTY_CRITERIA =
            new DiscoverySearchRequest(null, null, null, null, null, null, null, null, null, null);

    private final DiscoveryCollectionRepository discoveryCollectionRepository;
    private final DiscoveryService discoveryService;
    private final ObjectMapper objectMapper;

    public DiscoveryCollectionService(
            DiscoveryCollectionRepository discoveryCollectionRepository, DiscoveryService discoveryService, ObjectMapper objectMapper) {
        this.discoveryCollectionRepository = discoveryCollectionRepository;
        this.discoveryService = discoveryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CollectionSummaryResponse> list() {
        return discoveryCollectionRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(collection -> new CollectionSummaryResponse(collection.getCode(), collection.getName(), collection.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectionDetailResponse getOne(AuthenticatedUser principal, String code, Integer page, Integer size) {
        DiscoveryCollection collection = discoveryCollectionRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new ApiException(ErrorCode.COLLECTION_NOT_FOUND, "Collection not found"));
        DiscoverySearchRequest criteria = parseCriteria(collection.getCriteria());
        var members = discoveryService.browseCollection(principal, criteria, page, size);
        return new CollectionDetailResponse(collection.getCode(), collection.getName(), collection.getDescription(), members);
    }

    private DiscoverySearchRequest parseCriteria(String criteria) {
        if (criteria == null || criteria.isBlank()) {
            return EMPTY_CRITERIA;
        }
        try {
            return objectMapper.readValue(criteria, DiscoverySearchRequest.class);
        } catch (RuntimeException exception) {
            // A malformed criteria row is an admin/content bug, not a client error - fail open to the broadest
            // discoverable pool rather than 500ing the endpoint for every viewer.
            return EMPTY_CRITERIA;
        }
    }
}
