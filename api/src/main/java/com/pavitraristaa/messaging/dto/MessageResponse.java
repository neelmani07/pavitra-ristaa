package com.pavitraristaa.messaging.dto;

import com.pavitraristaa.media.dto.MediaFileResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderUserId,
        String messageType,
        String content,
        UUID replyToMessageId,
        String status,
        Instant sentAt,
        Instant editedAt,
        List<MediaFileResponse> attachments
) {
}
