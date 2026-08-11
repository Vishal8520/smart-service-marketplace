package com.example.marketplace.controller;

import com.example.marketplace.dto.request.PaymentOrderRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Razorpay payment order creation and webhook verification")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a Razorpay order for a booking (Customer role required)")
    public ResponseEntity<PaymentResponse> createOrder(
            @Valid @RequestBody PaymentOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createOrder(request, userDetails.getUsername()));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay payment success/failure webhook receiver")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, String> payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
