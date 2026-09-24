package com.pavitraristaa.connections.event;

import com.pavitraristaa.connections.entity.Interest;

/** Published whenever a new interest is sent. Connections has no idea who listens; the notifications module
 *  listens to tell the receiver, keeping connections free of any dependency on notifications. */
public record InterestReceivedEvent(Interest interest) {
}
