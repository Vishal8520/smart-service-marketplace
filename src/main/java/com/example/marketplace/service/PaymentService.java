package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.port.PaymentGatewayPort;
import com.example.marketplace.repository.BookingRepository;
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
    private final EmailService emailService;
    private final PaymentGatewayPort paymentGatewayPort;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository,
            UserRepository userRepository, EmailService emailService,
            PaymentGatewayPort paymentGatewayPort) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Transactional
    public PaymentResponse createPaymentReference(PaymentRequest request, String currentUserEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Booking not found with ID: " + request.getBookingId()));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUserEmail));

        // Check customer ownership (unless admin)
        if (user.getRole() != RoleType.ADMIN && !booking.getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to submit payment for this booking");
        }

        // Idempotency / duplicate check
        Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(booking.getId());
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if (existing.getStatus() == PaymentStatus.CONFIRMED) {
                throw new IllegalStateException(
                        "Payment for booking #" + booking.getId() + " has already been confirmed");
            }
            // Update existing pending/rejected reference with new reference
            existing.setUpiReferenceId(request.getUpiReferenceId());
            existing.setStatus(PaymentStatus.PENDING);
            existing.setNotes(null);
            Payment saved = paymentRepository.save(existing);
            emailService.sendPaymentSubmittedNotification(saved);
            return mapToResponse(saved);
        }

        // Invoke extension port stub for order tracking
        String stubOrderId = paymentGatewayPort.createOrder(booking.getService().getPrice(), "INR",
                "BOOKING_" + booking.getId());
        log.info("Payment extension port returned order ID: {} for booking #{}", stubOrderId, booking.getId());

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getService().getPrice())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .upiReferenceId(request.getUpiReferenceId())
                .createdAt(Instant.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        emailService.sendPaymentSubmittedNotification(saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminEmail));

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setConfirmedAt(Instant.now());
        payment.setConfirmedByAdmin(admin);
        payment.setNotes(request != null && request.getNotes() != null ? request.getNotes()
                : "Payment verified via manual UPI reference");

        // Transition booking to CONFIRMED
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);
        emailService.sendPaymentConfirmedNotification(saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse rejectPayment(Long paymentId, PaymentRejectRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        payment.setStatus(PaymentStatus.REJECTED);
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        emailService.sendPaymentRejectedNotification(saved);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(
                        () -> new IllegalArgumentException("No payment record found for booking ID: " + bookingId));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findAllByStatus(status, pageable).map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .upiReferenceId(payment.getUpiReferenceId())
                .confirmedAt(payment.getConfirmedAt())
                .confirmedByAdminId(
                        payment.getConfirmedByAdmin() != null ? payment.getConfirmedByAdmin().getId() : null)
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
