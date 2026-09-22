package com.pavitraristaa.common.security;

import java.util.Collection;
import java.util.UUID;

public record AuthenticatedUser(Long id, UUID uuid, Collection<String> roles, Long sessionId) {
}
