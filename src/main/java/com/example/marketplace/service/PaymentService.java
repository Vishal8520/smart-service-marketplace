package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.dto.ai.FraudScoreResponseDto;
import com.example.marketplace.dto.ai.PaymentFeaturesDto;
import com.example.marketplace.port.PaymentGatewayPort;
import com.example.marketplace.service.ai.AiServicePort;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.PaymentAuditLogRepository;
import com.example.marketplace.repository.PaymentRepository;
import com.example.marketplace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Pattern UTR_12_DIGIT_PATTERN = Pattern.compile("^[0-9]{12}$");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentAuditLogRepository auditLogRepository;
    private final EmailService emailService;
    private final PaymentGatewayPort paymentGatewayPort;
    private final AiServicePort aiServicePort;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                          UserRepository userRepository, PaymentAuditLogRepository auditLogRepository,
                          EmailService emailService, PaymentGatewayPort paymentGatewayPort,
                          AiServicePort aiServicePort) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.emailService = emailService;
        this.paymentGatewayPort = paymentGatewayPort;
        this.aiServicePort = aiServicePort;
    }

    @Transactional
    public PaymentResponse createPaymentReference(PaymentRequest request, String currentUserEmail) {
        // 1. Validate UTR reference format (strict 12-digit numeric)
        if (request.getUpiReferenceId() == null || !UTR_12_DIGIT_PATTERN.matcher(request.getUpiReferenceId().trim()).matches()) {
            throw new IllegalArgumentException("Invalid UPI Reference ID: must be exactly a 12-digit UTR number");
        }
        String upiRef = request.getUpiReferenceId().trim();

        // 2. Check Idempotency Key
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Payment> idempotentPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (idempotentPayment.isPresent()) {
                log.info("Idempotent payment request matched for key: {}", request.getIdempotencyKey());
                return mapToResponse(idempotentPayment.get());
            }
        }

        // 3. Find booking & check customer ownership
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseGet(booking::getCustomer);

        if (user.getRole() == RoleType.CUSTOMER && !booking.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to submit payment for this booking");
        }

        // 4. Enforce UTR Uniqueness and reject reuse
        Optional<Payment> existingWithUpi = paymentRepository.findByUpiReferenceId(upiRef);
        if (existingWithUpi.isPresent() && !existingWithUpi.get().getBooking().getId().equals(booking.getId())) {
            throw new IllegalStateException("UPI Reference ID " + upiRef + " has already been used for another payment");
        }

        // 5. Invoke extension port stub for order tracking
        String stubOrderId = paymentGatewayPort.createOrder(booking.getService().getPrice(), "INR",
                "BOOKING_" + booking.getId());
        log.info("Payment extension port created order ID: {} for booking #{}", stubOrderId, booking.getId());

        // 6. Payment state machine transition: -> AUTO_CONFIRMED
        Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(booking.getId());
        Payment payment;
        String fromStatus = "NONE";

        if (existingPaymentOpt.isPresent()) {
            payment = existingPaymentOpt.get();
            fromStatus = payment.getStatus().name();
            if (!payment.getStatus().canTransitionTo(PaymentStatus.AUTO_CONFIRMED)) {
                throw new IllegalStateException("Cannot transition payment from " + payment.getStatus() + " to AUTO_CONFIRMED");
            }
            payment.setUpiReferenceId(upiRef);
            payment.setStatus(PaymentStatus.AUTO_CONFIRMED);
            payment.setAutoConfirmed(true);
            payment.setConfirmedAt(Instant.now());
            payment.setNotes("Auto-confirmed via reference submission. Awaiting Admin verification.");
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                payment.setIdempotencyKey(request.getIdempotencyKey().trim());
            }
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getService().getPrice())
                    .currency("INR")
                    .status(PaymentStatus.AUTO_CONFIRMED)
                    .autoConfirmed(true)
                    .upiReferenceId(upiRef)
                    .idempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                            ? request.getIdempotencyKey().trim() : null)
                    .confirmedAt(Instant.now())
                    .notes("Auto-confirmed via reference submission. Awaiting Admin verification.")
                    .createdAt(Instant.now())
                    .build();
        }

        Payment saved = paymentRepository.save(payment);

        // Immutable Audit Log
        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .paymentId(saved.getId())
                .fromStatus(fromStatus)
                .toStatus(PaymentStatus.AUTO_CONFIRMED.name())
                .action("AUTO_CONFIRM")
                .actorEmail(currentUserEmail)
                .notes("Payment reference submitted and auto-confirmed. Provider contact remains locked until VERIFIED.")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        // AI Fraud / Anomaly Evaluation
        try {
            PaymentFeaturesDto features = new PaymentFeaturesDto();
            features.setPaymentId(saved.getId());
            features.setUserId(booking.getCustomer() != null ? booking.getCustomer().getId() : 0L);
            features.setAmount(saved.getAmount() != null ? saved.getAmount().doubleValue() : 0.0);
            features.setExpectedAmount(booking.getService() != null && booking.getService().getPrice() != null
                    ? booking.getService().getPrice().doubleValue() : features.getAmount());
            features.setUpiReferenceId(upiRef);
            features.setVelocityLast10m(1);
            features.setAccountAgeHours(72.0);
            features.setConsecutiveFailedPayments(0);
            features.setIsReferenceReused(false);

            FraudScoreResponseDto fraudScore = aiServicePort.scorePaymentFraud(features);
            if (fraudScore != null) {
                PaymentAuditLog aiAuditLog = PaymentAuditLog.builder()
                        .paymentId(saved.getId())
                        .fromStatus(PaymentStatus.AUTO_CONFIRMED.name())
                        .toStatus(PaymentStatus.AUTO_CONFIRMED.name())
                        .action("AI_FRAUD_SCORING")
                        .actorEmail("system-ai")
                        .notes(String.format("Risk Score: %.2f | Level: %s | Flagged: %s | Reasons: %s",
                                fraudScore.getRiskScore(), fraudScore.getRiskLevel(),
                                fraudScore.getFlaggedForReview(), String.join("; ", fraudScore.getReasons())))
                        .createdAt(Instant.now())
                        .build();
                auditLogRepository.save(aiAuditLog);

                if (Boolean.TRUE.equals(fraudScore.getFlaggedForReview())) {
                    log.warn("HIGH RISK payment detected for Payment #{}! Risk Score: {}. Reasons: {}",
                            saved.getId(), fraudScore.getRiskScore(), fraudScore.getReasons());
                }
            }
        } catch (Exception e) {
            log.warn("AI Fraud Scoring bypassed: {}", e.getMessage());
        }

        log.info("Payment #{} for booking #{} transitioned to AUTO_CONFIRMED by {}", saved.getId(), booking.getId(),
                currentUserEmail);

        // Notify user that payment is received and under review
        emailService.sendPaymentSubmittedNotification(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        // State Machine transition check: AUTO_CONFIRMED -> VERIFIED
        if (!payment.getStatus().canTransitionTo(PaymentStatus.VERIFIED)) {
            throw new IllegalStateException("Cannot transition payment from " + payment.getStatus() + " to VERIFIED");
        }

        String fromStatus = payment.getStatus().name();
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setConfirmedAt(Instant.now());
        payment.setConfirmedByAdmin(admin);
        payment.setNotes(request != null && request.getNotes() != null ? request.getNotes()
                : "Payment verified by Admin");

        // Unlock provider contact details by transitioning booking to CONFIRMED
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);

        // Immutable Audit Log
        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .paymentId(saved.getId())
                .fromStatus(fromStatus)
                .toStatus(PaymentStatus.VERIFIED.name())
                .action("ADMIN_VERIFY")
                .actorEmail(adminEmail)
                .notes(saved.getNotes())
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Payment #{} VERIFIED by admin {}. Provider contact details unlocked for booking #{}",
                saved.getId(), adminEmail, booking.getId());

        emailService.sendPaymentConfirmedNotification(saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse reversePayment(Long paymentId, PaymentRejectRequest request, String adminEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        // State Machine transition check: -> REVERSED
        if (!payment.getStatus().canTransitionTo(PaymentStatus.REVERSED)) {
            throw new IllegalStateException("Cannot transition payment from " + payment.getStatus() + " to REVERSED");
        }

        String fromStatus = payment.getStatus().name();
        String reason = (request != null && request.getNotes() != null) ? request.getNotes()
                : "Payment reversed by Admin audit";

        payment.setStatus(PaymentStatus.REVERSED);
        payment.setReversedAt(Instant.now());
        payment.setReversedByAdmin(admin);
        payment.setReversalReason(reason);
        payment.setNotes(reason);

        // Revert linked booking to PENDING and lock contact details
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);

        // Immutable Audit Log
        PaymentAuditLog auditLog = PaymentAuditLog.builder()
                .paymentId(saved.getId())
                .fromStatus(fromStatus)
                .toStatus(PaymentStatus.REVERSED.name())
                .action("ADMIN_REVERSE")
                .actorEmail(adminEmail)
                .notes(reason)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        log.warn("Payment #{} for booking #{} REVERSED by admin {}. Reason: {}", saved.getId(), booking.getId(),
                adminEmail, reason);

        emailService.sendPaymentRejectedNotification(saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse rejectPayment(Long paymentId, PaymentRejectRequest request, String adminEmail) {
        return reversePayment(paymentId, request, adminEmail);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, String currentUserEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        validatePaymentAccess(payment, currentUserEmail);
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId, String currentUserEmail) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for booking", "id", bookingId));
        validatePaymentAccess(payment, currentUserEmail);
        return mapToResponse(payment);
    }

    private void validatePaymentAccess(Payment payment, String currentUserEmail) {
        if (currentUserEmail == null || currentUserEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required to view payment details");
        }
        User user = userRepository.findByEmail(currentUserEmail).orElse(null);
        if (user != null && user.getRole() == RoleType.ADMIN) {
            return;
        }
        Booking booking = payment.getBooking();
        if (user == null || booking == null || !booking.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied: You can only view your own payment details");
        }
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        if (status == null) {
            return getAllPayments(pageable);
        }
        return paymentRepository.findAllByStatus(status, pageable).map(this::mapToResponse);
    }

    public PaymentResponse mapToResponse(Payment payment) {
        Booking booking = payment.getBooking();
        String customerName = (booking != null && booking.getCustomer() != null)
                ? booking.getCustomer().getName()
                : "Customer";

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .customerName(customerName)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .upiReferenceId(payment.getUpiReferenceId())
                .idempotencyKey(payment.getIdempotencyKey())
                .confirmedAt(payment.getConfirmedAt())
                .confirmedByAdminId(payment.getConfirmedByAdmin() != null ? payment.getConfirmedByAdmin().getId() : null)
                .autoConfirmed(payment.isAutoConfirmed())
                .reversedAt(payment.getReversedAt())
                .reversedByAdminId(payment.getReversedByAdmin() != null ? payment.getReversedByAdmin().getId() : null)
                .reversalReason(payment.getReversalReason())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
