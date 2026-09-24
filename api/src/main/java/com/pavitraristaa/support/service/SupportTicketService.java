package com.pavitraristaa.support.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.support.dto.CreateSupportTicketRequest;
import com.pavitraristaa.support.dto.SupportTicketResponse;
import com.pavitraristaa.support.dto.UpdateSupportTicketRequest;
import com.pavitraristaa.support.entity.SupportTicket;
import com.pavitraristaa.support.entity.TicketPriority;
import com.pavitraristaa.support.entity.TicketStatus;
import com.pavitraristaa.support.repository.SupportTicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketService {

    private final AuthService authService;
    private final SupportTicketRepository supportTicketRepository;

    public SupportTicketService(AuthService authService, SupportTicketRepository supportTicketRepository) {
        this.authService = authService;
        this.supportTicketRepository = supportTicketRepository;
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return supportTicketRepository.findByUserOrderByCreatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional
    public SupportTicketResponse create(AuthenticatedUser principal, CreateSupportTicketRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Instant now = Instant.now();
        SupportTicket ticket = new SupportTicket();
        ticket.setUuid(UUID.randomUUID());
        ticket.setUser(self);
        ticket.setCategory(request.category().trim());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(parsePriority(request.priority()));
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return toResponse(supportTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getOne(AuthenticatedUser principal, UUID ticketId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireOwned(self, ticketId));
    }

    @Transactional
    public SupportTicketResponse update(AuthenticatedUser principal, UUID ticketId, UpdateSupportTicketRequest request) {
        UserAccount self = authService.requireUsable(principal);
        SupportTicket ticket = requireOwned(self, ticketId);
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This ticket is already resolved and cannot be edited");
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description().trim());
            ticket.setUpdatedAt(Instant.now());
            supportTicketRepository.save(ticket);
        }
        return toResponse(ticket);
    }

    private SupportTicket requireOwned(UserAccount self, UUID ticketId) {
        SupportTicket ticket = supportTicketRepository.findByUuid(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUPPORT_TICKET_NOT_FOUND, "Support ticket not found"));
        if (!ticket.getUser().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.SUPPORT_TICKET_NOT_FOUND, "Support ticket not found");
        }
        return ticket;
    }

    private TicketPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TicketPriority.NORMAL;
        }
        try {
            return TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid priority", Map.of("priority", priority));
        }
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getUuid(),
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getResolvedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
