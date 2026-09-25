package com.pavitraristaa.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageReadEvent(UUID messageId, UUID readerUserId, Instant readAt) {
}
