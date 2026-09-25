package com.pavitraristaa.messaging.dto;

/** ChatEvent payload for SYSTEM - delivered back to the sender only, for a request the socket itself accepted
 *  but that failed validation (e.g. "not a participant", "conversation is closed"). HTTP has status codes for
 *  this; STOMP frames don't carry one, so the error travels as a normal event over the same queue instead. */
public record ChatSystemEvent(String code, String message) {
}
