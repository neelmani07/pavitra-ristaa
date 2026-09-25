package com.pavitraristaa.messaging.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.event.MatchActivatedEvent;
import com.pavitraristaa.connections.event.MatchEndedEvent;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.mapper.MediaFileMapper;
import com.pavitraristaa.messaging.dto.ConversationResponse;
import com.pavitraristaa.messaging.dto.UpdateConversationRequest;
import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.ConversationParticipant;
import com.pavitraristaa.messaging.entity.ConversationStatus;
import com.pavitraristaa.messaging.repository.ConversationParticipantRepository;
import com.pavitraristaa.messaging.repository.ConversationRepository;
import com.pavitraristaa.messaging.repository.MessageAttachmentRepository;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.trust.dto.ReportResponse;
import com.pavitraristaa.trust.entity.Block;
import com.pavitraristaa.trust.repository.BlockRepository;
import com.pavitraristaa.trust.service.ReportService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final AuthService authService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSummaryMapper userSummaryMapper;
    private final MediaFileMapper mediaFileMapper;
    private final BlockRepository blockRepository;
    private final ReportService reportService;

    public ConversationService(
            AuthService authService,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            UserProfileRepository userProfileRepository,
            UserSummaryMapper userSummaryMapper,
            MediaFileMapper mediaFileMapper,
            BlockRepository blockRepository,
            ReportService reportService
    ) {
        this.authService = authService;
        this.conversationRepository = conversationRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSummaryMapper = userSummaryMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.blockRepository = blockRepository;
        this.reportService = reportService;
    }

    // --- Reacts to the connections module forming/ending a match. Connections has no dependency on messaging;
    // this listener is how a conversation comes to exist at all, per "acceptance creates a match, followed by
    // conversation" in the product docs. Runs in the same transaction as the match save (default @EventListener
    // is synchronous), so a conversation always exists whenever its match does. ---

    @EventListener
    @Transactional
    public void onMatchActivated(MatchActivatedEvent event) {
        Match match = event.match();
        Instant now = Instant.now();
        Conversation conversation = conversationRepository.findByMatch(match).orElseGet(() -> {
            Conversation created = new Conversation();
            created.setUuid(UUID.randomUUID());
            created.setMatch(match);
            created.setCreatedAt(now);
            return created;
        });
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setUpdatedAt(now);
        Conversation saved = conversationRepository.save(conversation);
        ensureParticipant(saved, match.getUserA(), now);
        ensureParticipant(saved, match.getUserB(), now);
    }

    @EventListener
    @Transactional
    public void onMatchEnded(MatchEndedEvent event) {
        conversationRepository.findByMatch(event.match()).ifPresent(conversation -> {
            conversation.setStatus(ConversationStatus.CLOSED);
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
        });
    }

    private void ensureParticipant(Conversation conversation, UserAccount user, Instant now) {
        conversationParticipantRepository.findByConversationAndUser(conversation, user).ifPresentOrElse(
                existing -> existing.setLeftAt(null),
                () -> {
                    ConversationParticipant participant = new ConversationParticipant();
                    participant.setConversation(conversation);
                    participant.setUser(user);
                    participant.setJoinedAt(now);
                    conversationParticipantRepository.save(participant);
                });
    }

    // --- REST API ---

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return conversationParticipantRepository
                .findByUserOrderByConversationUpdatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(cp -> toResponse(cp.getConversation()))
                .getContent();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getOne(AuthenticatedUser principal, UUID conversationId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireParticipant(self, conversationId));
    }

    @Transactional
    public ConversationResponse updateStatus(AuthenticatedUser principal, UUID conversationId, UpdateConversationRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = requireParticipant(self, conversationId);
        if (request.status() != null) {
            conversation.setStatus(parseStatus(request.status()));
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
        }
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<MediaFileResponse> media(AuthenticatedUser principal, UUID conversationId, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = requireParticipant(self, conversationId);
        return messageAttachmentRepository.findByConversation(conversation, PaginationSupport.pageable(page, size))
                .map(attachment -> mediaFileMapper.toResponse(attachment.getMediaFile()))
                .getContent();
    }

    @Transactional
    public void blockParticipant(AuthenticatedUser principal, UUID conversationId) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = requireParticipant(self, conversationId);
        UserAccount other = otherParticipant(conversation, self);

        boolean alreadyBlocked = blockRepository.existsEitherDirection(self, other);
        if (!alreadyBlocked) {
            Block block = new Block();
            block.setBlocker(self);
            block.setBlocked(other);
            block.setCreatedAt(Instant.now());
            blockRepository.save(block);
        }
        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
    }

    @Transactional
    public ReportResponse reportParticipant(AuthenticatedUser principal, UUID conversationId, Long reasonId, String details) {
        UserAccount self = authService.requireUsable(principal);
        Conversation conversation = requireParticipant(self, conversationId);
        UserAccount other = otherParticipant(conversation, self);
        return reportService.create(principal, other.getUuid(), null, reasonId, details);
    }

    Conversation requireParticipant(UserAccount self, UUID conversationId) {
        Conversation conversation = conversationRepository.findByUuid(conversationId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND, "Conversation not found"));
        boolean participant = conversationParticipantRepository.existsByConversationAndUser(conversation, self);
        if (!participant) {
            throw new ApiException(ErrorCode.CONVERSATION_NOT_FOUND, "Conversation not found");
        }
        return conversation;
    }

    /** Package-visible so a new message bumps the conversation to the top of listMine()'s ordering. */
    void touch(Conversation conversation, Instant now) {
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
    }

    /** Package-visible so MessageService can block-check the recipient before a real-time send. */
    UserAccount otherParticipant(Conversation conversation, UserAccount self) {
        return conversationParticipantRepository.findByConversation(conversation).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(self.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND, "Conversation not found"));
    }

    /** Package-visible so MessageService knows who to broadcast a real-time event to. */
    List<UserAccount> participantsOf(Conversation conversation) {
        return conversationParticipantRepository.findByConversation(conversation).stream()
                .map(ConversationParticipant::getUser)
                .toList();
    }

    private ConversationStatus parseStatus(String status) {
        try {
            return ConversationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid status", Map.of("status", status));
        }
    }

    ConversationResponse toResponse(Conversation conversation) {
        List<UserSummaryResponse> participants = conversationParticipantRepository.findByConversation(conversation).stream()
                .map(ConversationParticipant::getUser)
                .map(user -> userProfileRepository.findByUserAndDeletedFalse(user)
                        .map(userSummaryMapper::toSummary)
                        .orElse(null))
                .filter(summary -> summary != null)
                .toList();
        return new ConversationResponse(
                conversation.getUuid(),
                conversation.getMatch() == null ? null : conversation.getMatch().getUuid(),
                conversation.getStatus().name(),
                participants,
                conversation.getUpdatedAt()
        );
    }
}
