package com.pavitraristaa.messaging.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.entity.MediaStatus;
import com.pavitraristaa.media.mapper.MediaFileMapper;
import com.pavitraristaa.media.repository.MediaFileRepository;
import com.pavitraristaa.messaging.dto.ChatEvent;
import com.pavitraristaa.messaging.dto.ChatEventType;
import com.pavitraristaa.messaging.dto.MessageDeletedEvent;
import com.pavitraristaa.messaging.dto.MessageDeliveredEvent;
import com.pavitraristaa.messaging.dto.MessageReadEvent;
import com.pavitraristaa.messaging.dto.MessageResponse;
import com.pavitraristaa.messaging.dto.MessageSentEvent;
import com.pavitraristaa.messaging.dto.ReactionRequest;
import com.pavitraristaa.messaging.dto.ReactionUpdatedEvent;
import com.pavitraristaa.messaging.dto.SendMessageRequest;
import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.ConversationStatus;
import com.pavitraristaa.messaging.entity.Message;
import com.pavitraristaa.messaging.entity.MessageAttachment;
import com.pavitraristaa.messaging.entity.MessageReaction;
import com.pavitraristaa.messaging.entity.MessageRead;
import com.pavitraristaa.messaging.entity.MessageStatus;
import com.pavitraristaa.messaging.entity.MessageType;
import com.pavitraristaa.messaging.repository.MessageAttachmentRepository;
import com.pavitraristaa.messaging.repository.MessageReactionRepository;
import com.pavitraristaa.messaging.repository.MessageReadRepository;
import com.pavitraristaa.messaging.repository.MessageRepository;
import com.pavitraristaa.trust.repository.BlockRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns both persistence AND the live-broadcast side effect of every state-changing action, regardless of
 * whether it was triggered over REST (delete, react, mark-read) or WebSocket (send, mark-read) - one place
 * decides "who gets told about this change", so a REST caller's delete/reaction is exactly as live as a
 * WebSocket one. SimpMessagingTemplate/SimpUserRegistry are plain Spring messaging infrastructure, not
 * controller/entity types, so this doesn't cross the "controllers don't touch entities" boundary - only
 * ChatWebSocketController does inbound STOMP handling; this class never parses a STOMP frame itself.
 */
@Service
public class MessageService {

    private final AuthService authService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReadRepository messageReadRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final BlockRepository blockRepository;
    private final MediaFileMapper mediaFileMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    public MessageService(
            AuthService authService,
            ConversationService conversationService,
            MessageRepository messageRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            MessageReadRepository messageReadRepository,
            MessageReactionRepository messageReactionRepository,
            MediaFileRepository mediaFileRepository,
            BlockRepository blockRepository,
            MediaFileMapper mediaFileMapper,
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry simpUserRegistry
    ) {
        this.authService = authService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.messageReadRepository = messageReadRepository;
        this.messageReactionRepository = messageReactionRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.blockRepository = blockRepository;
        this.mediaFileMapper = mediaFileMapper;
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    // Wide-open, in-range-for-Postgres sentinels standing in for "no bound" - see MessageRepository.findHistory.
    private static final Instant DISTANT_FUTURE = Instant.parse("9999-12-31T23:59:59Z");
    private static final Instant DISTANT_PAST = Instant.parse("0001-01-01T00:00:00Z");

    @Transactional(readOnly = true)
    public List<MessageResponse> history(
            AuthenticatedUser principal, UUID conversationId, Instant before, Instant after, Integer limit
    ) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Page<Message> page = messageRepository.findHistory(
                conversation,
                before == null ? DISTANT_FUTURE : before,
                after == null ? DISTANT_PAST : after,
                PaginationSupport.pageable(0, limit == null ? 50 : limit));
        return page.map(this::toResponse).getContent();
    }

    @Transactional(readOnly = true)
    public MessageResponse getOne(AuthenticatedUser principal, UUID conversationId, UUID messageId) {
        UserAccount self = authService.requireUsable(principal);
        conversationService.requireParticipant(self, conversationId);
        return toResponse(requireInConversation(conversationId, messageId));
    }

    /**
     * The WebSocket send path (contract section 5's client->server frame). Persists the message, then
     * broadcasts MESSAGE_SENT to every participant (the sender's own copy carries clientMessageId back for
     * optimistic-UI reconciliation; everyone else's doesn't), then - for whichever recipients are connected to
     * a live session right this moment - marks delivered and broadcasts MESSAGE_DELIVERED. A disconnected
     * recipient gets neither; they'll simply see it in history next time they load it.
     */
    @Transactional
    public MessageResponse send(AuthenticatedUser principal, SendMessageRequest request) {
        UserAccount self = authService.requireUsable(principal);
        if (request.conversationId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "conversationId is required");
        }
        Conversation conversation = conversationService.requireParticipant(self, request.conversationId());
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This conversation is not active");
        }
        UserAccount other = conversationService.otherParticipant(conversation, self);
        if (blockRepository.existsEitherDirection(self, other)) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "You cannot message this user");
        }

        MessageType messageType = parseMessageType(request.messageType());
        String content = blankToNull(request.content());
        List<UUID> attachmentIds = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        if (content == null && attachmentIds.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A message needs content or at least one attachment");
        }

        Message replyTo = request.replyToMessageId() == null
                ? null
                : requireInConversation(conversation.getUuid(), request.replyToMessageId());

        Instant now = Instant.now();
        Message message = new Message();
        message.setUuid(UUID.randomUUID());
        message.setConversation(conversation);
        message.setSender(self);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setReplyToMessage(replyTo);
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(now);
        Message saved = messageRepository.save(message);

        short order = 0;
        for (UUID attachmentId : attachmentIds) {
            MediaFile media = mediaFileRepository.findByUuidAndOwnerAndStatus(attachmentId, self, MediaStatus.ACTIVE)
                    .orElseThrow(() -> new ApiException(ErrorCode.MEDIA_NOT_FOUND, "Attachment not found"));
            MessageAttachment attachment = new MessageAttachment();
            attachment.setMessage(saved);
            attachment.setMediaFile(media);
            attachment.setDisplayOrder(order++);
            messageAttachmentRepository.save(attachment);
        }

        conversationService.touch(conversation, now);

        MessageResponse response = toResponse(saved);
        broadcast(conversation, self.getUuid(), ChatEventType.MESSAGE_SENT, new MessageSentEvent(response, request.clientMessageId()));

        for (UserAccount participant : conversationService.participantsOf(conversation)) {
            if (participant.getId().equals(self.getId())) {
                continue;
            }
            if (simpUserRegistry.getUser(participant.getUuid().toString()) != null) {
                markDelivered(saved, participant);
                Instant deliveredAt = Instant.now();
                broadcast(conversation, null, ChatEventType.MESSAGE_DELIVERED,
                        new MessageDeliveredEvent(saved.getUuid(), participant.getUuid(), deliveredAt));
            }
        }

        return response;
    }

    @Transactional
    public void delete(AuthenticatedUser principal, UUID conversationId, UUID messageId) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        if (!message.getSender().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the sender can delete this message");
        }
        message.setStatus(MessageStatus.DELETED);
        message.setDeletedAt(Instant.now());
        message.setContent(null);
        messageRepository.save(message);
        broadcast(conversation, null, ChatEventType.MESSAGE_DELETED, new MessageDeletedEvent(message.getUuid()));
    }

    @Transactional
    public void markRead(AuthenticatedUser principal, UUID conversationId, UUID messageId) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        Instant readAt = markRead(message, self);
        broadcast(conversation, null, ChatEventType.MESSAGE_READ, new MessageReadEvent(message.getUuid(), self.getUuid(), readAt));
    }

    @Transactional
    public void markConversationRead(AuthenticatedUser principal, UUID conversationId) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Page<Message> unreadCandidates = messageRepository.findHistory(
                conversation, DISTANT_FUTURE, DISTANT_PAST, PaginationSupport.pageable(0, 200));
        for (Message message : unreadCandidates) {
            if (!message.getSender().getId().equals(self.getId())) {
                Instant readAt = markRead(message, self);
                broadcast(conversation, null, ChatEventType.MESSAGE_READ,
                        new MessageReadEvent(message.getUuid(), self.getUuid(), readAt));
            }
        }
    }

    @Transactional
    public void react(AuthenticatedUser principal, UUID conversationId, UUID messageId, ReactionRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        MessageReaction reaction = messageReactionRepository.findByMessageAndUser(message, self).orElseGet(() -> {
            MessageReaction created = new MessageReaction();
            created.setMessage(message);
            created.setUser(self);
            created.setCreatedAt(Instant.now());
            return created;
        });
        String reactionCode = request.reactionCode().trim().toUpperCase(Locale.ROOT);
        reaction.setReactionCode(reactionCode);
        messageReactionRepository.save(reaction);
        broadcast(conversation, null, ChatEventType.REACTION_UPDATED,
                new ReactionUpdatedEvent(message.getUuid(), self.getUuid(), reactionCode));
    }

    /** Not itself broadcast - delivery is only meaningful as an immediate side effect of send(), where the
     *  sender's own connection also needs the delivered-tick event. */
    private void markDelivered(Message message, UserAccount recipient) {
        MessageRead read = messageReadRepository.findByMessageAndUser(message, recipient).orElseGet(() -> {
            MessageRead created = new MessageRead();
            created.setMessage(message);
            created.setUser(recipient);
            return created;
        });
        if (read.getDeliveredAt() == null) {
            read.setDeliveredAt(Instant.now());
            messageReadRepository.save(read);
        }
    }

    /** Returns the moment read was recorded, for the broadcast event. */
    private Instant markRead(Message message, UserAccount self) {
        Instant now = Instant.now();
        MessageRead read = messageReadRepository.findByMessageAndUser(message, self).orElseGet(() -> {
            MessageRead created = new MessageRead();
            created.setMessage(message);
            created.setUser(self);
            return created;
        });
        if (read.getDeliveredAt() == null) {
            read.setDeliveredAt(now);
        }
        read.setReadAt(now);
        messageReadRepository.save(read);
        return now;
    }

    /**
     * senderUuidForOwnCopy is non-null only for MESSAGE_SENT: that sender's own copy of the event keeps
     * clientMessageId (for optimistic-UI reconciliation), every other participant's copy has it stripped to
     * null. Every other event type broadcasts the identical payload to all participants (pass null).
     */
    private void broadcast(Conversation conversation, UUID senderUuidForOwnCopy, ChatEventType type, Object payload) {
        for (UserAccount participant : conversationService.participantsOf(conversation)) {
            Object payloadForThisParticipant = payload;
            if (senderUuidForOwnCopy != null && payload instanceof MessageSentEvent sent
                    && !participant.getUuid().equals(senderUuidForOwnCopy)) {
                payloadForThisParticipant = new MessageSentEvent(sent.message(), null);
            }
            ChatEvent event = new ChatEvent(type, conversation.getUuid(), payloadForThisParticipant);
            messagingTemplate.convertAndSendToUser(participant.getUuid().toString(), "/queue/chat", event);
        }
    }

    private MessageType parseMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return MessageType.TEXT;
        }
        try {
            return MessageType.valueOf(messageType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid message type", Map.of("messageType", messageType));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Message requireInConversation(UUID conversationId, UUID messageId) {
        Message message = messageRepository.findByUuid(messageId)
                .orElseThrow(() -> new ApiException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found"));
        if (!message.getConversation().getUuid().equals(conversationId)) {
            throw new ApiException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found");
        }
        return message;
    }

    private MessageResponse toResponse(Message message) {
        List<MediaFileResponse> attachments = messageAttachmentRepository
                .findByMessageOrderByDisplayOrderAsc(message).stream()
                .map(MessageAttachment::getMediaFile)
                .map(mediaFileMapper::toResponse)
                .toList();
        return new MessageResponse(
                message.getUuid(),
                message.getConversation().getUuid(),
                message.getSender().getUuid(),
                message.getMessageType().name(),
                message.getContent(),
                message.getReplyToMessage() == null ? null : message.getReplyToMessage().getUuid(),
                message.getStatus().name(),
                message.getSentAt(),
                message.getEditedAt(),
                attachments
        );
    }
}
