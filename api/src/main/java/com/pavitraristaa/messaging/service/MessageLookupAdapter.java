package com.pavitraristaa.messaging.service;

import com.pavitraristaa.messaging.repository.MessageRepository;
import com.pavitraristaa.trust.service.MessageLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MessageLookupAdapter implements MessageLookup {

    private final MessageRepository messageRepository;

    public MessageLookupAdapter(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Optional<Long> internalIdOf(UUID messageUuid) {
        return messageRepository.findByUuid(messageUuid).map(message -> message.getId());
    }
}
