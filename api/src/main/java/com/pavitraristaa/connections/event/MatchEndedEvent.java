package com.pavitraristaa.connections.event;

import com.pavitraristaa.connections.entity.Match;

/** Published when a match is unmatched. Messaging listens to close the matching conversation. */
public record MatchEndedEvent(Match match) {
}
