package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.messaging.entity.Message;
import com.pavitraristaa.messaging.entity.MessageReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageAndUser(Message message, UserAccount user);
}
