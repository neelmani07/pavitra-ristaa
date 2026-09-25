package com.pavitraristaa.messaging.dto;

import java.util.UUID;

/** Client->server payload for /app/chat.read - a STOMP frame has no URL path params, so both ids travel in the body. */
public record MarkMessageReadRequest(UUID conversationId, UUID messageId) {
}
