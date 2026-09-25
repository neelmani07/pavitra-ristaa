package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.subscriptions.dto.InvoiceResponse;
import com.pavitraristaa.subscriptions.entity.Invoice;
import com.pavitraristaa.subscriptions.entity.InvoiceStatus;
import com.pavitraristaa.subscriptions.entity.Payment;
import com.pavitraristaa.subscriptions.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final AuthService authService;
    private final InvoiceRepository invoiceRepository;

    public InvoiceService(AuthService authService, InvoiceRepository invoiceRepository) {
        this.authService = authService;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Called by PaymentService right after a charge succeeds. No tax logic exists yet - tax_amount is always
     * zero, total_amount equals amount - deferred until there's a real tax requirement to implement against.
     * invoice_number is derived from the invoice's own uuid (the table has no sequence to format against
     * without an extra round trip) - readable enough, and the column's own UNIQUE constraint is the real
     * safety net if it ever collided.
     */
    @Transactional
    Invoice issueForPayment(Payment payment) {
        UUID uuid = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setUuid(uuid);
        invoice.setUser(payment.getUser());
        invoice.setSubscription(payment.getSubscription());
        invoice.setPayment(payment);
        invoice.setInvoiceNumber("INV-" + uuid.toString().substring(0, 8).toUpperCase(Locale.ROOT));
        invoice.setAmount(payment.getAmount());
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(payment.getAmount());
        invoice.setCurrencyCode(payment.getCurrencyCode());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setIssuedAt(Instant.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return invoiceRepository.findByUserOrderByIssuedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getOne(AuthenticatedUser principal, UUID invoiceId) {
        UserAccount self = authService.requireUsable(principal);
        Invoice invoice = invoiceRepository.findByUuid(invoiceId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND, "Invoice not found"));
        if (!invoice.getUser().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.INVOICE_NOT_FOUND, "Invoice not found");
        }
        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getUuid(), invoice.getInvoiceNumber(), invoice.getAmount(), invoice.getTaxAmount(),
                invoice.getTotalAmount(), invoice.getCurrencyCode(), invoice.getStatus().name(), invoice.getIssuedAt());
    }
}
