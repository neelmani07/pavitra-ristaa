package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.Message;
import com.pavitraristaa.messaging.entity.MessageAttachment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    @EntityGraph(attributePaths = {"mediaFile"})
    @Query("""
            select a from MessageAttachment a
            where a.message.conversation = :conversation and a.message.status <> com.pavitraristaa.messaging.entity.MessageStatus.DELETED
            order by a.message.sentAt desc
            """)
    Page<MessageAttachment> findByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

    @EntityGraph(attributePaths = {"mediaFile"})
    List<MessageAttachment> findByMessageOrderByDisplayOrderAsc(Message message);
}
