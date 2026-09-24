package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminSupportTicketResponse;
import com.pavitraristaa.admin.dto.AssignTicketRequest;
import com.pavitraristaa.admin.dto.ResolveTicketRequest;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.support.entity.SupportTicket;
import com.pavitraristaa.support.entity.TicketStatus;
import com.pavitraristaa.support.repository.SupportTicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin-side counterpart of support.SupportTicketService: sees every user's tickets, can assign and resolve. */
@Service
public class AdminSupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public AdminSupportService(
            SupportTicketRepository supportTicketRepository, UserAccountRepository userAccountRepository, AuditLogService auditLogService) {
        this.supportTicketRepository = supportTicketRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminSupportTicketResponse> list(String status, Integer page, Integer size) {
        var pageable = PaginationSupport.pageable(page, size);
        Page<SupportTicket> result = status == null || status.isBlank()
                ? supportTicketRepository.findAllByOrderByCreatedAtDesc(pageable)
                : supportTicketRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        List<AdminSupportTicketResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminSupportTicketResponse assign(AuthenticatedUser principal, UUID ticketId, AssignTicketRequest request) {
        SupportTicket ticket = requireTicket(ticketId);
        UserAccount admin = actingAdmin(principal);
        UserAccount assignee = request == null || request.assigneeId() == null
                ? admin
                : userAccountRepository.findByUuid(request.assigneeId())
                        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignee not found"));
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(Instant.now());
        SupportTicket saved = supportTicketRepository.save(ticket);
        auditLogService.record(admin, "SUPPORT_TICKET_ASSIGNED", "support_ticket", saved.getId(), Map.of("assigneeId", assignee.getUuid()));
        return toResponse(saved);
    }

    @Transactional
    public AdminSupportTicketResponse resolve(AuthenticatedUser principal, UUID ticketId, ResolveTicketRequest request) {
        SupportTicket ticket = requireTicket(ticketId);
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This ticket is already closed");
        }
        Instant now = Instant.now();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(now);
        ticket.setUpdatedAt(now);
        if (request != null && request.resolutionNotes() != null && !request.resolutionNotes().isBlank()) {
            ticket.setDescription(ticket.getDescription() + "\n---\nResolution: " + request.resolutionNotes().trim());
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        UserAccount admin = actingAdmin(principal);
        auditLogService.record(admin, "SUPPORT_TICKET_RESOLVED", "support_ticket", saved.getId(), null);
        return toResponse(saved);
    }

    private TicketStatus parseStatus(String status) {
        try {
            return TicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid ticket status", Map.of("status", status));
        }
    }

    private SupportTicket requireTicket(UUID ticketId) {
        return supportTicketRepository.findByUuid(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUPPORT_TICKET_NOT_FOUND, "Support ticket not found"));
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminSupportTicketResponse toResponse(SupportTicket ticket) {
        return new AdminSupportTicketResponse(
                ticket.getUuid(),
                ticket.getUser().getUuid(),
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getUuid(),
                ticket.getResolvedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
