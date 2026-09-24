package com.pavitraristaa.admin.dto;

import java.util.UUID;

/** assigneeId is optional - omit it to assign the ticket to the acting admin/moderator themselves. */
public record AssignTicketRequest(UUID assigneeId) {
}
