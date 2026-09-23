package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    boolean existsByConversationAndUser(Conversation conversation, UserAccount user);

    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, UserAccount user);

    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversation(Conversation conversation);

    @EntityGraph(attributePaths = {"conversation", "conversation.match"})
    @Query("""
            select cp from ConversationParticipant cp
            where cp.user = :user
            order by cp.conversation.updatedAt desc
            """)
    Page<ConversationParticipant> findByUserOrderByConversationUpdatedAtDesc(@Param("user") UserAccount user, Pageable pageable);
}
