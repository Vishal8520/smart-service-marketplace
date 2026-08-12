package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.port.PaymentGatewayPort;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.PaymentAuditLogRepository;
import com.example.marketplace.repository.PaymentRepository;
import com.example.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentAuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService notificationService;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @InjectMocks
    private PaymentService paymentService;

    private Booking booking;
    private Payment payment;
    private User admin;
    private User customer;
    private User provider;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(10L).name("Customer John").email("customer@example.com").build();
        provider = User.builder().id(20L).name("Provider Jane").email("provider@example.com").build();
        admin = User.builder().id(1L).name("Admin User").email("vishalghasoliya22@gmail.com").role(RoleType.ADMIN)
                .build();

        ServiceListing serviceListing = ServiceListing.builder().id(5L).title("House Cleaning")
                .price(new BigDecimal("1500.00")).provider(provider).build();

        booking = Booking.builder()
                .id(100L)
                .customer(customer)
                .service(serviceListing)
                .status(BookingStatus.PENDING)
                .build();

        payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(new BigDecimal("1500.00"))
                .upiReferenceId("user@upi")
                .status(PaymentStatus.AUTO_CONFIRMED)
                .autoConfirmed(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create payment and immediately auto-confirm it in Demo mode")
    void createPaymentReference_AutoConfirmsImmediately() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(100L);
        request.setUpiReferenceId("user@upi");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.createPaymentReference(request, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTO_CONFIRMED);
        assertThat(response.isAutoConfirmed()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(auditLogRepository, times(1)).save(any(PaymentAuditLog.class));
        verify(notificationService, times(1)).sendPaymentConfirmedNotification(any());
    }

    @Test
    @DisplayName("Should successfully reverse an auto-confirmed payment and audit the transition")
    void reversePayment_Success() {
        PaymentRejectRequest request = new PaymentRejectRequest();
        request.setNotes("Amount mismatch found in audit");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("vishalghasoliya22@gmail.com")).thenReturn(Optional.of(admin));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.reversePayment(1L, request, "vishalghasoliya22@gmail.com");

        assertThat(response).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(payment.getReversalReason()).isEqualTo("Amount mismatch found in audit");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(auditLogRepository, times(1)).save(any(PaymentAuditLog.class));
        verify(notificationService, times(1)).sendPaymentRejectedNotification(any());
    }
}
