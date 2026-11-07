package com.example.marketplace.controller;

import com.example.marketplace.dto.ai.AdminFeedbackRequestDto;
import com.example.marketplace.dto.request.CityRequest;
import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.response.AdminAnalyticsResponse;
import com.example.marketplace.dto.response.CityResponse;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.KycStatus;
import com.example.marketplace.entity.PaymentStatus;
import com.example.marketplace.entity.ServiceStatus;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.UserRepository;
import com.example.marketplace.service.AdminService;
import com.example.marketplace.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard analytics, service approvals, KYC management, payment confirmations, and city management")
public class AdminController {

    private final AdminService adminService;
    private final CityService cityService;
    private final UserRepository userRepository;
    private final com.example.marketplace.service.ai.AiServicePort aiServicePort;

    public AdminController(AdminService adminService, CityService cityService, UserRepository userRepository,
                           com.example.marketplace.service.ai.AiServicePort aiServicePort) {
        this.adminService = adminService;
        this.cityService = cityService;
        this.userRepository = userRepository;
        this.aiServicePort = aiServicePort;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get high-level marketplace analytics (revenue, users, bookings, ratings)")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all registered platform users with pagination")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    // --- Provider KYC Verification ---

    @GetMapping("/providers/kyc")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List providers filtered by KYC status")
    public ResponseEntity<Page<User>> getProvidersByKyc(
            @RequestParam(defaultValue = "PENDING") KycStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getProvidersByKycStatus(status, pageable));
    }

    @PutMapping("/providers/{id}/kyc/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a service provider KYC application")
    public ResponseEntity<User> approveProviderKyc(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveProviderKyc(id));
    }

    @PutMapping("/providers/{id}/kyc/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a service provider KYC application")
    public ResponseEntity<User> rejectProviderKyc(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Documents did not meet verification criteria") String reason) {
        return ResponseEntity.ok(adminService.rejectProviderKyc(id, reason));
    }

    // --- Service Approvals ---

    @GetMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List services filtered by approval status (e.g. PENDING_REVIEW)")
    public ResponseEntity<Page<ServiceResponse>> getServicesByStatus(
            @RequestParam(defaultValue = "PENDING_REVIEW") ServiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminService.getServicesByStatus(status, pageable));
    }

    @PutMapping("/services/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a pending service listing (verifies provider KYC)")
    public ResponseEntity<ServiceResponse> approveService(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveService(id));
    }

    @PutMapping("/services/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a pending service listing")
    public ResponseEntity<ServiceResponse> rejectService(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectService(id));
    }

    @PutMapping("/services/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle active/inactive state of a service listing")
    public ResponseEntity<ServiceResponse> toggleServiceActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleServiceActive(id));
    }

    // --- Payment Verification & Audit ---

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all payment records with optional status filter")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminService.getPaymentsByStatus(status, pageable));
    }

    @PostMapping("/payments/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify manual UPI payment reference and unlock customer/provider contact (AUTO_CONFIRMED -> VERIFIED)")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentConfirmRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        return ResponseEntity.ok(adminService.confirmPayment(id, request, authentication.getName()));
    }

    @PostMapping("/payments/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reverse a payment with reason note (transitions to REVERSED and re-locks contact)")
    public ResponseEntity<PaymentResponse> reversePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRejectRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        return ResponseEntity.ok(adminService.reversePayment(id, request, authentication.getName()));
    }

    @PostMapping("/payments/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject manual UPI payment reference with reason note")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRejectRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        return ResponseEntity.ok(adminService.rejectPayment(id, request, authentication.getName()));
    }

    // --- City Management ---

    @GetMapping("/cities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all platform cities (active and inactive)")
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @PostMapping("/cities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new city for service discovery")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    @PutMapping("/cities/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle active state of a city")
    public ResponseEntity<CityResponse> toggleCityActive(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.toggleCityActive(id));
    }

    // --- AI Intelligence Observability & Feedback ---

    @PostMapping("/ai-feedback")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Submit admin evaluation feedback on AI decisions (fraud flags, review sentiment, ranking)")
    public ResponseEntity<Void> submitAiFeedback(@Valid @RequestBody AdminFeedbackRequestDto request) {
        aiServicePort.submitFeedback(request);
        return ResponseEntity.ok().build();
    }
}
