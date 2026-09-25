package com.pavitraristaa.messaging.dto;

/** The server event types from the API contract's WebSocket chat section. MESSAGE_EDITED is listed there but
 *  has no producer yet - editing a sent message isn't built (REST has no edit endpoint either), deliberately
 *  out of scope for this pass; kept in the enum so a future edit feature has a type to use without a client
 *  protocol change. */
public enum ChatEventType {
    MESSAGE_SENT,
    MESSAGE_DELIVERED,
    MESSAGE_READ,
    MESSAGE_EDITED,
    MESSAGE_DELETED,
    REACTION_UPDATED,
    SYSTEM
}
