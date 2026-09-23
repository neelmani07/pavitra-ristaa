package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.messaging.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = {"match"})
    Optional<Conversation> findByUuid(UUID uuid);

    Optional<Conversation> findByMatch(Match match);
}
