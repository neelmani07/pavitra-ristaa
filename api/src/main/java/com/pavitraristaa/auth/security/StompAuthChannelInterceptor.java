package com.pavitraristaa.auth.security;

import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.security.StompPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * The WebSocket equivalent of JwtAuthenticationFilter. A raw browser WebSocket handshake can't carry a custom
 * Authorization header (that's a genuine browser limitation, not a choice made here), so per the contract
 * ("Authentication: JWT during WebSocket connection establishment") the token travels as a native STOMP header
 * on the CONNECT frame instead - the first message sent over the already-open socket, which STOMP client
 * libraries (e.g. @stomp/stompjs) let you set freely as connectHeaders. The raw HTTP upgrade request for
 * /ws/chat is left open in SecurityConfig for exactly this reason; this interceptor is the real gate.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing Authorization header on STOMP CONNECT");
            }
            try {
                Claims claims = jwtService.parse(header.substring(7));
                UUID userUuid = UUID.fromString(claims.getSubject());
                Long sessionId = JwtAuthenticationFilter.toLong(claims.get("sid"));
                Collection<String> roles = JwtAuthenticationFilter.extractRoles(claims);
                accessor.setUser(new StompPrincipal(new AuthenticatedUser(null, userUuid, roles, sessionId)));
            } catch (JwtException | IllegalArgumentException exception) {
                throw new MessagingException("Invalid or expired access token", exception);
            }
        }
        return message;
    }
}
