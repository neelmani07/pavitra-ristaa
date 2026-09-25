package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.subscriptions.service.RazorpayWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server, unauthenticated (verified by HMAC signature, not a JWT - see SecurityConfig's permitAll
 * for this exact path). @Hidden keeps it off the public Swagger UI, since it's not part of the app's own API
 * surface. All the actual handling lives in RazorpayWebhookService - this stays a thin adapter so it never
 * needs to touch entities directly (ModuleBoundariesTest.controllersDoNotExposeEntities).
 */
@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
@Hidden
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody String rawBody, @RequestHeader("X-Razorpay-Signature") String signature) {
        boolean verified = webhookService.handle(rawBody, signature);
        return verified ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
