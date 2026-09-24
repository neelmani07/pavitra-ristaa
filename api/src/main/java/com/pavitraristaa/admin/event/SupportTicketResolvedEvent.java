package com.pavitraristaa.admin.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published when an admin resolves a support ticket; notifies the ticket's owner. */
public record SupportTicketResolvedEvent(UserAccount ticketOwner) {
}
