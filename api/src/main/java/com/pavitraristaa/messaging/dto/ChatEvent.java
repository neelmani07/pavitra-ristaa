package com.pavitraristaa.messaging.dto;

import java.util.UUID;

/** The envelope every server->client frame is sent as, to /user/{uuid}/queue/chat. payload's shape depends on
 *  type: MessageSentEvent for MESSAGE_SENT, MessageDeliveredEvent for MESSAGE_DELIVERED, MessageReadEvent for
 *  MESSAGE_READ, MessageDeletedEvent for MESSAGE_DELETED, ReactionUpdatedEvent for REACTION_UPDATED,
 *  ChatSystemEvent for SYSTEM. */
public record ChatEvent(ChatEventType type, UUID conversationId, Object payload) {
}
