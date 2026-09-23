package com.pavitraristaa.connections.event;

import com.pavitraristaa.connections.entity.Match;

/**
 * Published whenever a match becomes (or becomes again) ACTIVE - both on first creation and when a previously
 * unmatched pair re-matches. Connections has no idea who listens; messaging listens to create/reopen the
 * matching conversation, keeping connections free of any dependency on messaging.
 */
public record MatchActivatedEvent(Match match) {
}
