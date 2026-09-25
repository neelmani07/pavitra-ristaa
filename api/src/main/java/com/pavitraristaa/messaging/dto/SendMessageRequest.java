package com.pavitraristaa.messaging.dto;

import java.util.List;
import java.util.UUID;

/**
 * The client->server payload for STOMP destination /app/chat.send - the WebSocket equivalent of a "create
 * message" request body, per the contract's own field list for this frame. clientMessageId is generated
 * client-side before the server confirms anything (for optimistic-UI rendering) and echoed back inside
 * MessageSentEvent so the sender's own client can reconcile its local placeholder with the server's real
 * message id/sentAt - only the sender's own copy of the event gets it back, never other participants'.
 */
public record SendMessageRequest(
        UUID conversationId,
        UUID clientMessageId,
        String messageType,
        String content,
        UUID replyToMessageId,
        List<UUID> attachmentIds
) {
}
