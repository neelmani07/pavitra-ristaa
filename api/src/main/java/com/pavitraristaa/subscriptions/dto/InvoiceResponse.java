package com.pavitraristaa.subscriptions.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        BigDecimal amount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currencyCode,
        String status,
        Instant issuedAt
) {
}
