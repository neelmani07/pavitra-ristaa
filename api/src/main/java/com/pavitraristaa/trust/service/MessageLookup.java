package com.pavitraristaa.trust.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a public message UUID to its internal id, for POST /reports' optional reportedMessageId - without
 * trust depending on the messaging module (messaging already depends on trust, for Block; the two must not
 * depend on each other). Implemented in messaging, consumed here as an interface only.
 */
public interface MessageLookup {

    Optional<Long> internalIdOf(UUID messageUuid);
}
