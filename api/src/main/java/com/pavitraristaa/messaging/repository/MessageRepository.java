package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.messaging.entity.Conversation;
import com.pavitraristaa.messaging.entity.Message;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "conversation", "replyToMessage"})
    Optional<Message> findByUuid(UUID uuid);

    // before/after are always non-null (the service substitutes wide-open sentinel bounds for "no filter"):
    // pgjdbc cannot always determine a bind parameter's type when it appears only inside an "IS NULL"-guarded
    // branch (the same issue DiscoveryProfileRepository hit and fixed by switching to Specifications - not
    // worth that machinery here for two plain comparisons, so this sidesteps it the simpler way instead).
    @EntityGraph(attributePaths = {"sender", "replyToMessage"})
    @Query("""
            select m from Message m
            where m.conversation = :conversation and m.sentAt < :before and m.sentAt > :after
            order by m.sentAt desc
            """)
    Page<Message> findHistory(
            @Param("conversation") Conversation conversation,
            @Param("before") Instant before,
            @Param("after") Instant after,
            Pageable pageable);
}
