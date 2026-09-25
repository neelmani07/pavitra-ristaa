package com.pavitraristaa.subscriptions.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAutoRenewRequest(@NotNull Boolean autoRenew) {
}
