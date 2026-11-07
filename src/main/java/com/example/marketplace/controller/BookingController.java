package com.example.marketplace.controller;

import com.example.marketplace.dto.request.BookingRequest;
import com.example.marketplace.dto.request.BookingStatusRequest;
import com.example.marketplace.dto.response.BookingResponse;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Service booking management for customers and providers with strict ownership enforcement")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Book a service with customer details & address")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String authEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, authEmail));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID (Ownership protected)")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(bookingService.getBooking(id, authentication.getName()));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Initiate payment reference preparation for a booking")
    public ResponseEntity<BookingResponse> payBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String authEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(bookingService.payBooking(id, authEmail));
    }

    @GetMapping("/me")
    @Operation(summary = "Get bookings for currently authenticated customer with optional status filter & pagination")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required to view your bookings");
        }
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(bookingService.getCustomerBookingsWithStatus(authentication.getName(), status, pageable));
    }

    @GetMapping("/customer")
    @Operation(summary = "Get bookings for customer (Requires customer ownership or admin role)")
    public ResponseEntity<Page<BookingResponse>> getCustomerBookings(
            @RequestParam String email,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String authEmail = authentication != null ? authentication.getName() : null;
        if (authEmail == null || (!authEmail.equalsIgnoreCase(email) && !hasAdminRole(authentication))) {
            throw new UnauthorizedException("You are not authorized to view bookings for " + email);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getCustomerBookingsWithStatus(email, status, pageable));
    }

    @GetMapping("/provider")
    @Operation(summary = "Get incoming bookings for authenticated provider")
    public ResponseEntity<Page<BookingResponse>> getProviderBookings(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String providerEmail = authentication != null ? authentication.getName() : email;
        if (providerEmail == null || providerEmail.isBlank()) {
            throw new UnauthorizedException("Provider authentication required");
        }
        if (email != null && !email.equalsIgnoreCase(providerEmail) && !hasAdminRole(authentication)) {
            throw new UnauthorizedException("You are not authorized to view provider bookings for " + email);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getProviderBookings(providerEmail, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update booking status — Provider view")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Provider authentication required");
        }
        return ResponseEntity.ok(bookingService.updateStatus(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (Customer ownership enforced)")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Customer authentication required");
        }
        return ResponseEntity.ok(bookingService.cancelBooking(id, authentication.getName()));
    }

    private boolean hasAdminRole(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
