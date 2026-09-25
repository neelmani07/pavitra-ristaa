package com.pavitraristaa.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.security.JwtService;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.connections.dto.CreateInterestRequest;
import com.pavitraristaa.connections.dto.InterestResponse;
import com.pavitraristaa.connections.service.InterestService;
import com.pavitraristaa.messaging.dto.ChatEvent;
import com.pavitraristaa.messaging.dto.ChatEventType;
import com.pavitraristaa.messaging.dto.MarkMessageReadRequest;
import com.pavitraristaa.messaging.dto.MessageReadEvent;
import com.pavitraristaa.messaging.dto.MessageSentEvent;
import com.pavitraristaa.messaging.dto.SendMessageRequest;
import com.pavitraristaa.messaging.service.ConversationService;
import com.pavitraristaa.profile.dto.UpdateProfileRequest;
import com.pavitraristaa.profile.service.ProfileService;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The single test that exercises the whole live-chat subsystem end to end rather than just MessageService's
 * persistence logic in isolation (see AbstractPersistenceTests for that): two real STOMP-over-WebSocket
 * connections, JWT auth on the CONNECT frame (StompAuthChannelInterceptor), a live send that the other
 * participant actually receives (WebSocketConfig's broadcast routing), and a read receipt flowing back.
 * webEnvironment = RANDOM_PORT is what makes an actual embedded server exist to connect a real client to -
 * every other test in this codebase runs against a mock/no web environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ChatWebSocketIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @LocalServerPort
    private int port;

    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProfileService profileService;
    @Autowired private InterestService interestService;
    @Autowired private ConversationService conversationService;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void aSentMessageIsDeliveredLiveAndAReadReceiptFlowsBack() throws Exception {
        AuthenticatedUser alice = discoverableUser("FEMALE", 28, "DATING");
        AuthenticatedUser bob = discoverableUser("MALE", 30, "DATING");
        InterestResponse sent = interestService.send(alice, new CreateInterestRequest(bob.uuid(), "DATING", null));
        interestService.accept(bob, sent.id());
        UUID conversationId = conversationService.listMine(alice, null, null).get(0).id();

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        BlockingQueue<ChatEvent> bobEvents = new LinkedBlockingQueue<>();
        BlockingQueue<ChatEvent> aliceEvents = new LinkedBlockingQueue<>();

        StompSession bobSession = connect(stompClient, bob, bobEvents);
        StompSession aliceSession = connect(stompClient, alice, aliceEvents);
        try {
            UUID clientMessageId = UUID.randomUUID();
            aliceSession.send("/app/chat.send",
                    new SendMessageRequest(conversationId, clientMessageId, "TEXT", "Hi Bob, live!", null, null));

            // Bob receives MESSAGE_SENT live, without polling REST history.
            ChatEvent bobSent = bobEvents.poll(5, TimeUnit.SECONDS);
            assertThat(bobSent).isNotNull();
            assertThat(bobSent.type()).isEqualTo(ChatEventType.MESSAGE_SENT);
            MessageSentEvent bobPayload = convert(bobSent.payload(), MessageSentEvent.class);
            assertThat(bobPayload.message().content()).isEqualTo("Hi Bob, live!");
            assertThat(bobPayload.clientMessageId()).isNull(); // only the sender's own copy carries this
            UUID messageId = bobPayload.message().id();

            // Alice's own connection gets its copy back too, with clientMessageId for optimistic-UI reconciliation.
            ChatEvent aliceSent = aliceEvents.poll(5, TimeUnit.SECONDS);
            assertThat(aliceSent).isNotNull();
            assertThat(aliceSent.type()).isEqualTo(ChatEventType.MESSAGE_SENT);
            assertThat(convert(aliceSent.payload(), MessageSentEvent.class).clientMessageId()).isEqualTo(clientMessageId);

            // Both were connected at send time, so a MESSAGE_DELIVERED event follows for Bob (the recipient).
            ChatEvent delivered = pollUntilType(bobEvents, ChatEventType.MESSAGE_DELIVERED, 5);
            assertThat(delivered).isNotNull();

            // Bob marks it read over the socket; both connections hear MESSAGE_READ.
            bobSession.send("/app/chat.read", new MarkMessageReadRequest(conversationId, messageId));
            ChatEvent aliceRead = pollUntilType(aliceEvents, ChatEventType.MESSAGE_READ, 5);
            assertThat(aliceRead).isNotNull();
            MessageReadEvent readPayload = convert(aliceRead.payload(), MessageReadEvent.class);
            assertThat(readPayload.messageId()).isEqualTo(messageId);
            assertThat(readPayload.readerUserId()).isEqualTo(bob.uuid());
        } finally {
            aliceSession.disconnect();
            bobSession.disconnect();
        }
    }

    @Test
    void connectWithoutAValidTokenIsRejected() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer not-a-real-token");

        BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();
        stompClient.connectAsync(
                "ws://localhost:" + port + "/ws/chat",
                (org.springframework.web.socket.WebSocketHttpHeaders) null,
                connectHeaders,
                new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                failures.add(exception);
            }
        });

        assertThat(pollNotNull(failures, 5)).isTrue();
    }

    private StompSession connect(WebSocketStompClient stompClient, AuthenticatedUser user, BlockingQueue<ChatEvent> events) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtService.createAccessToken(user.uuid(), 1L, List.of("USER")));
        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws/chat",
                (org.springframework.web.socket.WebSocketHttpHeaders) null,
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
        session.subscribe("/user/queue/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                events.add((ChatEvent) payload);
            }
        });
        return session;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object payload, Class<T> type) {
        // The JacksonJsonMessageConverter deserializes ChatEvent.payload as a Map (Object-typed field, no
        // static type to target) - re-run it through the same conversion the server would have applied.
        Map<String, Object> map = (Map<String, Object>) payload;
        if (type == MessageSentEvent.class) {
            Map<String, Object> messageMap = (Map<String, Object>) map.get("message");
            var message = new com.pavitraristaa.messaging.dto.MessageResponse(
                    UUID.fromString((String) messageMap.get("id")),
                    UUID.fromString((String) messageMap.get("conversationId")),
                    UUID.fromString((String) messageMap.get("senderUserId")),
                    (String) messageMap.get("messageType"),
                    (String) messageMap.get("content"),
                    messageMap.get("replyToMessageId") == null ? null : UUID.fromString((String) messageMap.get("replyToMessageId")),
                    (String) messageMap.get("status"),
                    Instant.parse((String) messageMap.get("sentAt")),
                    messageMap.get("editedAt") == null ? null : Instant.parse((String) messageMap.get("editedAt")),
                    List.of());
            UUID clientMessageId = map.get("clientMessageId") == null ? null : UUID.fromString((String) map.get("clientMessageId"));
            return (T) new MessageSentEvent(message, clientMessageId);
        }
        if (type == MessageReadEvent.class) {
            return (T) new MessageReadEvent(
                    UUID.fromString((String) map.get("messageId")),
                    UUID.fromString((String) map.get("readerUserId")),
                    Instant.parse((String) map.get("readAt")));
        }
        throw new IllegalArgumentException("Unsupported payload type in this test: " + type);
    }

    private ChatEvent pollUntilType(BlockingQueue<ChatEvent> queue, ChatEventType type, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            ChatEvent event = queue.poll(500, TimeUnit.MILLISECONDS);
            if (event != null && event.type() == type) {
                return event;
            }
        }
        return null;
    }

    private boolean pollNotNull(BlockingQueue<Throwable> queue, int timeoutSeconds) {
        try {
            return queue.poll(timeoutSeconds, TimeUnit.SECONDS) != null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private AuthenticatedUser discoverableUser(String gender, int age, String modeCode) {
        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setUuid(UUID.randomUUID());
        user.setEmail(user.getUuid() + "@example.test");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setVersion(0L);
        UserAccount saved = userAccountRepository.save(user);
        AuthenticatedUser principal = new AuthenticatedUser(saved.getId(), saved.getUuid(), List.of("USER"), 1L);

        profileService.updateCore(principal, new UpdateProfileRequest(
                "Test", null, null, LocalDate.now().minusYears(age), gender, null, null, null, null, null));
        jdbcTemplate.update(
                "update user_profile set profile_status = 'ACTIVE' where user_id = (select id from \"user\" where uuid = ?)",
                principal.uuid());
        jdbcTemplate.update(
                "insert into user_relationship_mode (user_id, relationship_mode_id, created_at) "
                        + "values ((select id from \"user\" where uuid = ?), "
                        + "(select id from relationship_mode where code = ?), now())",
                principal.uuid(), modeCode);
        return principal;
    }
}
