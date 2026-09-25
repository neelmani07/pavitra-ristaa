package com.pavitraristaa.config;

import com.pavitraristaa.auth.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over a plain WebSocket at /ws/chat (contract section 5's "proposed transport endpoint" - the exact
 * STOMP destinations were explicitly left for implementation to finalize). No SockJS fallback: this API serves
 * native mobile clients as much as web, and every STOMP-capable client library (web or mobile) speaks plain
 * WebSocket directly - SockJS exists for older-browser HTTP-polling fallback this project doesn't need.
 *
 * The simple in-memory broker below is enough for one instance; running more than one API instance behind a
 * load balancer would need a real broker relay (e.g. RabbitMQ STOMP, or Spring's Redis-backed broker) so an
 * event published on instance A reaches a user connected to instance B - not needed yet, worth knowing before
 * scaling horizontally.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PavitraProperties properties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(PavitraProperties properties, StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.properties = properties;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(properties.getCors().getAllowedOrigins().toArray(new String[0]));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
