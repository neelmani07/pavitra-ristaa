package com.pavitraristaa.messaging.dto;

import java.util.UUID;

public record MessageDeletedEvent(UUID messageId) {
}
