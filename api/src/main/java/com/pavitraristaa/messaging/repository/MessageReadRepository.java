package com.pavitraristaa.messaging.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.messaging.entity.Message;
import com.pavitraristaa.messaging.entity.MessageRead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    Optional<MessageRead> findByMessageAndUser(Message message, UserAccount user);
}
