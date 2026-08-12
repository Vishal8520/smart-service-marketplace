package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.port.PaymentGatewayPort;
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

@Service
public class PaymentService {

        private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

        private final PaymentRepository paymentRepository;
        private final BookingRepository bookingRepository;
        private final UserRepository userRepository;
        private final PaymentAuditLogRepository auditLogRepository;
        private final EmailService emailService;
        private final PaymentGatewayPort paymentGatewayPort;

        public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                        UserRepository userRepository, PaymentAuditLogRepository auditLogRepository,
                        EmailService emailService, PaymentGatewayPort paymentGatewayPort) {
                this.paymentRepository = paymentRepository;
                this.bookingRepository = bookingRepository;
                this.userRepository = userRepository;
                this.auditLogRepository = auditLogRepository;
                this.emailService = emailService;
                this.paymentGatewayPort = paymentGatewayPort;
        }

        @Transactional
        public PaymentResponse createPaymentReference(PaymentRequest request, String currentUserEmail) {
                Booking booking = bookingRepository.findById(request.getBookingId())
                                .orElseThrow(
                                                () -> new IllegalArgumentException("Booking not found with ID: "
                                                                + request.getBookingId()));

                User user = userRepository.findByEmail(currentUserEmail)
                                .orElseGet(() -> booking.getCustomer());

                // Check customer ownership (unless admin)
                if (user.getRole() == RoleType.CUSTOMER && !booking.getCustomer().getId().equals(user.getId())) {
                        throw new IllegalStateException("You are not authorized to submit payment for this booking");
                }

                // Invoke extension port stub for order tracking
                String stubOrderId = paymentGatewayPort.createOrder(booking.getService().getPrice(), "INR",
                                "BOOKING_" + booking.getId());
                log.info("Payment extension port created order ID: {} for booking #{}", stubOrderId, booking.getId());

                // Idempotency / duplicate check
                Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(booking.getId());
                Payment payment;
                String fromStatus = "NONE";

                if (existingPaymentOpt.isPresent()) {
                        payment = existingPaymentOpt.get();
                        fromStatus = payment.getStatus().name();
                        payment.setUpiReferenceId(request.getUpiReferenceId());
                        payment.setStatus(PaymentStatus.AUTO_CONFIRMED);
                        payment.setAutoConfirmed(true);
                        payment.setConfirmedAt(Instant.now());
                        payment.setNotes("Auto-confirmed via demo system");
                } else {
                        payment = Payment.builder()
                                        .booking(booking)
                                        .amount(booking.getService().getPrice())
                                        .currency("INR")
                                        .status(PaymentStatus.AUTO_CONFIRMED)
                                        .autoConfirmed(true)
                                        .upiReferenceId(request.getUpiReferenceId())
                                        .confirmedAt(Instant.now())
                                        .notes("Auto-confirmed via demo system")
                                        .createdAt(Instant.now())
                                        .build();
                }

                // Transition booking to CONFIRMED immediately
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                Payment saved = paymentRepository.save(payment);

                // Record Audit Log
                PaymentAuditLog auditLog = PaymentAuditLog.builder()
                                .paymentId(saved.getId())
                                .fromStatus(fromStatus)
                                .toStatus(PaymentStatus.AUTO_CONFIRMED.name())
                                .action("AUTO_CONFIRM")
                                .actorEmail(currentUserEmail)
                                .notes("Payment auto-approved on creation (Demo Mode)")
                                .createdAt(Instant.now())
                                .build();
                auditLogRepository.save(auditLog);

                log.info("Payment #{} for booking #{} AUTO_CONFIRMED by {}", saved.getId(), booking.getId(),
                                currentUserEmail);

                // Trigger instant payment confirmed email notification
                emailService.sendPaymentConfirmedNotification(saved);

                return mapToResponse(saved);
        }

        @Transactional
        public PaymentResponse confirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
                Payment payment = paymentRepository.findById(paymentId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Payment not found with ID: " + paymentId));

                User admin = userRepository.findByEmail(adminEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminEmail));

                String fromStatus = payment.getStatus().name();
                payment.setStatus(PaymentStatus.CONFIRMED);
                payment.setConfirmedAt(Instant.now());
                payment.setConfirmedByAdmin(admin);
                payment.setNotes(request != null && request.getNotes() != null ? request.getNotes()
                                : "Payment verified by Admin");

                // Transition booking to CONFIRMED
                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                Payment saved = paymentRepository.save(payment);

                PaymentAuditLog auditLog = PaymentAuditLog.builder()
                                .paymentId(saved.getId())
                                .fromStatus(fromStatus)
                                .toStatus(PaymentStatus.CONFIRMED.name())
                                .action("ADMIN_CONFIRM")
                                .actorEmail(adminEmail)
                                .notes(saved.getNotes())
                                .createdAt(Instant.now())
                                .build();
                auditLogRepository.save(auditLog);

                log.info("Payment #{} confirmed by admin {}", saved.getId(), adminEmail);
                emailService.sendPaymentConfirmedNotification(saved);
                return mapToResponse(saved);
        }

        @Transactional
        public PaymentResponse reversePayment(Long paymentId, PaymentRejectRequest request, String adminEmail) {
                Payment payment = paymentRepository.findById(paymentId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Payment not found with ID: " + paymentId));

                User admin = userRepository.findByEmail(adminEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminEmail));

                String fromStatus = payment.getStatus().name();
                String reason = (request != null && request.getNotes() != null) ? request.getNotes()
                                : "Payment reversed by Admin audit";

                payment.setStatus(PaymentStatus.REVERSED);
                payment.setReversedAt(Instant.now());
                payment.setReversedByAdmin(admin);
                payment.setReversalReason(reason);
                payment.setNotes(reason);

                // Revert linked booking to PENDING
                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.PENDING);
                bookingRepository.save(booking);

                Payment saved = paymentRepository.save(payment);

                // Record Audit Log
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
        public PaymentResponse rejectPayment(Long paymentId, PaymentRejectRequest request) {
                return reversePayment(paymentId, request, "vishalghasoliya22@gmail.com");
        }

        @Transactional(readOnly = true)
        public PaymentResponse getPaymentById(Long paymentId) {
                Payment payment = paymentRepository.findById(paymentId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Payment not found with ID: " + paymentId));
                return mapToResponse(payment);
        }

        @Transactional(readOnly = true)
        public PaymentResponse getPaymentByBookingId(Long bookingId) {
                Payment payment = paymentRepository.findByBookingId(bookingId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No payment record found for booking ID: "
                                                                                + bookingId));
                return mapToResponse(payment);
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

        private PaymentResponse mapToResponse(Payment payment) {
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
                                .confirmedAt(payment.getConfirmedAt())
                                .confirmedByAdminId(
                                                payment.getConfirmedByAdmin() != null
                                                                ? payment.getConfirmedByAdmin().getId()
                                                                : null)
                                .autoConfirmed(payment.isAutoConfirmed())
                                .reversedAt(payment.getReversedAt())
                                .reversedByAdminId(
                                                payment.getReversedByAdmin() != null
                                                                ? payment.getReversedByAdmin().getId()
                                                                : null)
                                .reversalReason(payment.getReversalReason())
                                .notes(payment.getNotes())
                                .createdAt(payment.getCreatedAt())
                                .build();
        }
}
