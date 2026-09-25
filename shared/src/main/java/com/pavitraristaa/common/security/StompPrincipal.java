package com.pavitraristaa.common.security;

import java.security.Principal;

/**
 * Wraps AuthenticatedUser as a java.security.Principal so Spring's STOMP messaging infrastructure can carry it
 * through a WebSocket session the same way SecurityContextHolder carries it through an HTTP request. getName()
 * is the user's UUID - Spring's convertAndSendToUser()/@SendToUser routing uses Principal.getName() as the
 * destination key (/user/{name}/queue/...), so this is what makes "send an event to this specific user"
 * possible regardless of which feature originates the event. Lives in shared, not in any one feature package,
 * for the same reason AuthenticatedUser itself does - any feature may need it, not just messaging.
 */
public record StompPrincipal(AuthenticatedUser authenticatedUser) implements Principal {

    @Override
    public String getName() {
        return authenticatedUser.uuid().toString();
    }
}
