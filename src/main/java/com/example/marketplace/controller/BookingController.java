package com.example.marketplace.controller;

import com.example.marketplace.dto.request.BookingRequest;
import com.example.marketplace.dto.request.BookingStatusRequest;
import com.example.marketplace.dto.response.BookingResponse;
import com.example.marketplace.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Service booking management for customers and providers")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Book a service with customer details & address (Passwordless Flow)")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, request.getCustomerEmail()));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Simulate payment completion — Unlocks Provider Contact Info (Server-Side)")
    public ResponseEntity<BookingResponse> payBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.payBooking(id));
    }

    @GetMapping("/customer")
    @Operation(summary = "Get bookings for customer by email")
    public ResponseEntity<Page<BookingResponse>> getCustomerBookings(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.getCustomerBookings(email, pageable));
    }

    @GetMapping("/provider")
    @Operation(summary = "Get incoming bookings for provider by email")
    public ResponseEntity<Page<BookingResponse>> getProviderBookings(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.getProviderBookings(email, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update booking status — Provider view")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusRequest request,
            @RequestParam(required = false) String providerEmail) {
        return ResponseEntity.ok(bookingService.updateStatus(id, request, providerEmail));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String customerEmail) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, customerEmail));
    }
}
