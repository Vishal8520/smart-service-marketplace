package com.example.marketplace.controller;

import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Manual UPI reference-based payment submission and retrieval")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Submit manual UPI reference ID for a booking")
    public ResponseEntity<PaymentResponse> createPaymentReference(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        String currentUserEmail = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "customer.guest@marketplace.com";

        PaymentResponse response = paymentService.createPaymentReference(request, currentUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment reference details by Payment ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment reference details by Booking ID")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }
}
