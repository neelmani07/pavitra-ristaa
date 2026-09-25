package com.pavitraristaa.messaging.dto;

import java.util.UUID;

/** ChatEvent payload for MESSAGE_SENT. clientMessageId is only populated in the copy delivered back to the
 *  sender's own connection(s) - see SendMessageRequest's own note. */
public record MessageSentEvent(MessageResponse message, UUID clientMessageId) {
}
