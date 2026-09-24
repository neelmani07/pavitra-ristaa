package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AuditLogResponse;
import com.pavitraristaa.admin.entity.AuditLog;
import com.pavitraristaa.admin.repository.AuditLogRepository;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.util.PaginationSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Called by every admin write action, so GET /admin/audit-logs actually has content. */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(UserAccount actor, String action, String entityType, Long entityId, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setCreatedAt(Instant.now());
        if (metadata != null && !metadata.isEmpty()) {
            try {
                entry.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (RuntimeException exception) {
                log.warn("Could not serialize audit log metadata for action {}", action);
            }
        }
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PagedData<AuditLogResponse> list(Integer page, Integer size) {
        Page<AuditLog> result = auditLogRepository.findAllByOrderByCreatedAtDesc(PaginationSupport.pageable(page, size));
        List<AuditLogResponse> items = result.getContent().stream()
                .map(entry -> new AuditLogResponse(
                        entry.getId(),
                        entry.getActor() == null ? null : entry.getActor().getUuid(),
                        entry.getAction(),
                        entry.getEntityType(),
                        entry.getEntityId(),
                        entry.getMetadata(),
                        entry.getCreatedAt()))
                .toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
