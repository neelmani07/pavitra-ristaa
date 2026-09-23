package com.pavitraristaa.messaging.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.mapper.MediaFileMapper;
import com.pavitraristaa.messaging.dto.MessageResponse;
import com.pavitraristaa.messaging.dto.ReactionRequest;
import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.Message;
import com.pavitraristaa.messaging.entity.MessageAttachment;
import com.pavitraristaa.messaging.entity.MessageReaction;
import com.pavitraristaa.messaging.entity.MessageRead;
import com.pavitraristaa.messaging.entity.MessageStatus;
import com.pavitraristaa.messaging.repository.MessageAttachmentRepository;
import com.pavitraristaa.messaging.repository.MessageReactionRepository;
import com.pavitraristaa.messaging.repository.MessageReadRepository;
import com.pavitraristaa.messaging.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final AuthService authService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReadRepository messageReadRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MediaFileMapper mediaFileMapper;

    public MessageService(
            AuthService authService,
            ConversationService conversationService,
            MessageRepository messageRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            MessageReadRepository messageReadRepository,
            MessageReactionRepository messageReactionRepository,
            MediaFileMapper mediaFileMapper
    ) {
        this.authService = authService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.messageReadRepository = messageReadRepository;
        this.messageReactionRepository = messageReactionRepository;
        this.mediaFileMapper = mediaFileMapper;
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

    @Transactional
    public void delete(AuthenticatedUser principal, UUID conversationId, UUID messageId) {
        UserAccount self = authService.requireUsable(principal);
        conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        if (!message.getSender().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the sender can delete this message");
        }
        message.setStatus(MessageStatus.DELETED);
        message.setDeletedAt(Instant.now());
        message.setContent(null);
        messageRepository.save(message);
    }

    @Transactional
    public void markRead(AuthenticatedUser principal, UUID conversationId, UUID messageId) {
        UserAccount self = authService.requireUsable(principal);
        conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        markRead(message, self);
    }

    @Transactional
    public void markConversationRead(AuthenticatedUser principal, UUID conversationId) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = conversationService.requireParticipant(self, conversationId);
        Page<Message> unreadCandidates = messageRepository.findHistory(
                conversation, DISTANT_FUTURE, DISTANT_PAST, PaginationSupport.pageable(0, 200));
        for (Message message : unreadCandidates) {
            if (!message.getSender().getId().equals(self.getId())) {
                markRead(message, self);
            }
        }
    }

    @Transactional
    public void react(AuthenticatedUser principal, UUID conversationId, UUID messageId, ReactionRequest request) {
        UserAccount self = authService.requireUsable(principal);
        conversationService.requireParticipant(self, conversationId);
        Message message = requireInConversation(conversationId, messageId);
        MessageReaction reaction = messageReactionRepository.findByMessageAndUser(message, self).orElseGet(() -> {
            MessageReaction created = new MessageReaction();
            created.setMessage(message);
            created.setUser(self);
            created.setCreatedAt(Instant.now());
            return created;
        });
        reaction.setReactionCode(request.reactionCode().trim().toUpperCase());
        messageReactionRepository.save(reaction);
    }

    private void markRead(Message message, UserAccount self) {
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
