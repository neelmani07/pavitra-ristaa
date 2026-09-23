package com.pavitraristaa.messaging.dto;

import jakarta.validation.constraints.NotNull;

public record ReportConversationRequest(@NotNull Long reasonId, String details) {
}
