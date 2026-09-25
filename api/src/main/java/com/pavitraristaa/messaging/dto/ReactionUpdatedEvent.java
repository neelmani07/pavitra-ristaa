package com.pavitraristaa.messaging.dto;

import java.util.UUID;

public record ReactionUpdatedEvent(UUID messageId, UUID userId, String reactionCode) {
}
