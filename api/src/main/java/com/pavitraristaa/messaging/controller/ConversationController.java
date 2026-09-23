package com.pavitraristaa.messaging.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.messaging.dto.ConversationResponse;
import com.pavitraristaa.messaging.dto.MessageResponse;
import com.pavitraristaa.messaging.dto.ReactionRequest;
import com.pavitraristaa.messaging.dto.ReportConversationRequest;
import com.pavitraristaa.messaging.dto.UpdateConversationRequest;
import com.pavitraristaa.messaging.service.ConversationService;
import com.pavitraristaa.messaging.service.MessageService;
import com.pavitraristaa.trust.dto.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST side of messaging only: conversation/message history and management. Sending a message is WebSocket-only
 * per the API contract (there is no POST .../messages endpoint) and is not implemented - the STOMP destinations
 * are explicitly unfinalized in the project's own docs ("finalize before chat implementation"), so there is
 * nothing yet to build against. A conversation comes into existence automatically when a match is made
 * (see ConversationService's match-event listeners), so message history is empty until sending exists.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Messaging")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CurrentUserAccessor currentUserAccessor;

    public ConversationController(
            ConversationService conversationService, MessageService messageService, CurrentUserAccessor currentUserAccessor
    ) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List conversations")
    public ApiResponse<List<ConversationResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(conversationService.listMine(currentUserAccessor.requireUser(), page, size), "Conversations");
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details")
    public ApiResponse<ConversationResponse> getOne(@PathVariable UUID conversationId) {
        return ApiResponse.ok(conversationService.getOne(currentUserAccessor.requireUser(), conversationId), "Conversation");
    }

    @PutMapping("/{conversationId}")
    @Operation(summary = "Update conversation settings/status")
    public ApiResponse<ConversationResponse> update(
            @PathVariable UUID conversationId, @RequestBody UpdateConversationRequest request) {
        return ApiResponse.ok(
                conversationService.updateStatus(currentUserAccessor.requireUser(), conversationId, request),
                "Conversation updated");
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get paginated message history")
    public ApiResponse<List<MessageResponse>> messages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok(
                messageService.history(currentUserAccessor.requireUser(), conversationId, before, after, limit),
                "Messages");
    }

    @GetMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Get one message")
    public ApiResponse<MessageResponse> getMessage(@PathVariable UUID conversationId, @PathVariable UUID messageId) {
        return ApiResponse.ok(
                messageService.getOne(currentUserAccessor.requireUser(), conversationId, messageId), "Message");
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Delete a message")
    public ApiResponse<Void> deleteMessage(@PathVariable UUID conversationId, @PathVariable UUID messageId) {
        messageService.delete(currentUserAccessor.requireUser(), conversationId, messageId);
        return ApiResponse.ok("Message deleted");
    }

    @PostMapping("/{conversationId}/messages/{messageId}/read")
    @Operation(summary = "Mark message as read")
    public ApiResponse<Void> markMessageRead(@PathVariable UUID conversationId, @PathVariable UUID messageId) {
        messageService.markRead(currentUserAccessor.requireUser(), conversationId, messageId);
        return ApiResponse.ok("Marked as read");
    }

    @PostMapping("/{conversationId}/read")
    @Operation(summary = "Mark conversation messages as read")
    public ApiResponse<Void> markConversationRead(@PathVariable UUID conversationId) {
        messageService.markConversationRead(currentUserAccessor.requireUser(), conversationId);
        return ApiResponse.ok("Conversation marked as read");
    }

    @PutMapping("/{conversationId}/messages/{messageId}/reaction")
    @Operation(summary = "Add or replace a message reaction")
    public ApiResponse<Void> react(
            @PathVariable UUID conversationId, @PathVariable UUID messageId, @Valid @RequestBody ReactionRequest request) {
        messageService.react(currentUserAccessor.requireUser(), conversationId, messageId, request);
        return ApiResponse.ok("Reaction saved");
    }

    @GetMapping("/{conversationId}/media")
    @Operation(summary = "List shared media")
    public ApiResponse<List<MediaFileResponse>> media(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                conversationService.media(currentUserAccessor.requireUser(), conversationId, page, size), "Shared media");
    }

    @PostMapping("/{conversationId}/report")
    @Operation(summary = "Report conversation/user from chat")
    public ApiResponse<ReportResponse> report(
            @PathVariable UUID conversationId, @Valid @RequestBody ReportConversationRequest request) {
        return ApiResponse.ok(
                conversationService.reportParticipant(
                        currentUserAccessor.requireUser(), conversationId, request.reasonId(), request.details()),
                "Report submitted");
    }

    @PostMapping("/{conversationId}/block")
    @Operation(summary = "Block participant from chat")
    public ApiResponse<Void> block(@PathVariable UUID conversationId) {
        conversationService.blockParticipant(currentUserAccessor.requireUser(), conversationId);
        return ApiResponse.ok("Participant blocked");
    }
}
