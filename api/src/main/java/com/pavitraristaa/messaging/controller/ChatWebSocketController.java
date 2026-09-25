package com.pavitraristaa.messaging.controller;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.security.StompPrincipal;
import com.pavitraristaa.messaging.dto.ChatEvent;
import com.pavitraristaa.messaging.dto.ChatEventType;
import com.pavitraristaa.messaging.dto.ChatSystemEvent;
import com.pavitraristaa.messaging.dto.MarkMessageReadRequest;
import com.pavitraristaa.messaging.dto.SendMessageRequest;
import com.pavitraristaa.messaging.service.MessageService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * Inbound STOMP handling only - thin on purpose. All persistence and the outbound broadcast (to every
 * participant, not just the caller) lives in MessageService, so a message sent here is exactly as durable and
 * as live as one deleted/reacted-to over plain REST. Never touches an entity directly (see MessageService's own
 * class comment), keeping ModuleBoundariesTest.controllersDoNotExposeEntities intact for this controller too.
 */
@Controller
public class ChatWebSocketController {

    private final MessageService messageService;

    public ChatWebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        messageService.send(authenticatedUser(principal), request);
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload MarkMessageReadRequest request, Principal principal) {
        messageService.markRead(authenticatedUser(principal), request.conversationId(), request.messageId());
    }

    /** Routed back to just the user whose frame caused it (Spring resolves this from the STOMP session, not
     *  from a method parameter) - STOMP has no HTTP-style status code, so an error is just another event on
     *  the same queue the caller already listens on. */
    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/chat")
    public ChatEvent handleApiException(ApiException exception) {
        return new ChatEvent(ChatEventType.SYSTEM, null, new ChatSystemEvent(exception.getErrorCode().name(), exception.getMessage()));
    }

    private AuthenticatedUser authenticatedUser(Principal principal) {
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }
        return stompPrincipal.authenticatedUser();
    }
}
