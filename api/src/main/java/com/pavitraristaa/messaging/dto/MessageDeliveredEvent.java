package com.pavitraristaa.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ChatEvent payload for MESSAGE_DELIVERED. Emitted immediately after a successful send, for each recipient who
 * is connected to a live WebSocket session at that moment (via SimpUserRegistry) - a pragmatic proxy for "this
 * reached a live client", not a true device-level delivery receipt (there is no per-device ack round trip).
 * Skipped entirely for a recipient who isn't connected right now; they'll simply see the message when history
 * next loads, with no separate "delivered" event needed.
 */
public record MessageDeliveredEvent(UUID messageId, UUID recipientUserId, Instant deliveredAt) {
}
