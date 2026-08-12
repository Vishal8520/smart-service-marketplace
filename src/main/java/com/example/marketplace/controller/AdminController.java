package com.example.marketplace.controller;

import com.example.marketplace.dto.request.CityRequest;
import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.response.AdminAnalyticsResponse;
import com.example.marketplace.dto.response.CityResponse;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.PaymentStatus;
import com.example.marketplace.entity.ServiceStatus;
import com.example.marketplace.entity.User;
import com.example.marketplace.repository.UserRepository;
import com.example.marketplace.service.AdminService;
import com.example.marketplace.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard analytics, service approvals, payment confirmations, and city management")
public class AdminController {

    private final AdminService adminService;
    private final CityService cityService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, CityService cityService, UserRepository userRepository) {
        this.adminService = adminService;
        this.cityService = cityService;
        this.userRepository = userRepository;
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get high-level marketplace analytics (revenue, users, bookings, ratings)")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered platform users with pagination")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    // --- Service Approvals ---

    @GetMapping("/services")
    @Operation(summary = "List services filtered by approval status (e.g. PENDING_REVIEW)")
    public ResponseEntity<Page<ServiceResponse>> getServicesByStatus(
            @RequestParam(defaultValue = "PENDING_REVIEW") ServiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getServicesByStatus(status, pageable));
    }

    @PutMapping("/services/{id}/approve")
    @Operation(summary = "Approve a pending service listing")
    public ResponseEntity<ServiceResponse> approveService(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveService(id));
    }

    @PutMapping("/services/{id}/reject")
    @Operation(summary = "Reject a pending service listing")
    public ResponseEntity<ServiceResponse> rejectService(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectService(id));
    }

    @PutMapping("/services/{id}/toggle-active")
    @Operation(summary = "Toggle active/inactive state of a service listing")
    public ResponseEntity<ServiceResponse> toggleServiceActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleServiceActive(id));
    }

    // --- Payment Verification & Audit ---

    @GetMapping("/payments")
    @Operation(summary = "List all payment records with optional status filter")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getPaymentsByStatus(status, pageable));
    }

    @PostMapping("/payments/{id}/confirm")
    @Operation(summary = "Confirm manual UPI payment reference and unlock customer/provider contact")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentConfirmRequest request,
            Authentication authentication) {
        String adminEmail = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "vishalghasoliya22@gmail.com";
        return ResponseEntity.ok(adminService.confirmPayment(id, request, adminEmail));
    }

    @PostMapping("/payments/{id}/reverse")
    @Operation(summary = "Reverse an auto-confirmed or confirmed payment with reason note")
    public ResponseEntity<PaymentResponse> reversePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRejectRequest request,
            Authentication authentication) {
        String adminEmail = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "vishalghasoliya22@gmail.com";
        return ResponseEntity.ok(adminService.reversePayment(id, request, adminEmail));
    }

    @PostMapping("/payments/{id}/reject")
    @Operation(summary = "Reject manual UPI payment reference with reason note")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRejectRequest request) {
        return ResponseEntity.ok(adminService.rejectPayment(id, request));
    }

    // --- City Management ---

    @GetMapping("/cities")
    @Operation(summary = "Get all platform cities (active and inactive)")
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @PostMapping("/cities")
    @Operation(summary = "Create a new city for service discovery")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    @PutMapping("/cities/{id}/toggle")
    @Operation(summary = "Toggle active state of a city")
    public ResponseEntity<CityResponse> toggleCityActive(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.toggleCityActive(id));
    }
}
