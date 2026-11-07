package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.request.PaymentRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.UnauthorizedException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private EmailService emailService;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private com.example.marketplace.service.ai.AiServicePort aiServicePort;

    @InjectMocks
    private PaymentService paymentService;

    private Booking booking;
    private Payment payment;
    private User admin;
    private User customer;
    private User provider;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(10L).name("Customer John").email("customer@example.com").role(RoleType.CUSTOMER).build();
        provider = User.builder().id(20L).name("Provider Jane").email("provider@example.com").role(RoleType.SERVICE_PROVIDER).build();
        admin = User.builder().id(1L).name("Admin User").email("admin@servenow.com").role(RoleType.ADMIN).build();

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
                .upiReferenceId("123456789012")
                .status(PaymentStatus.AUTO_CONFIRMED)
                .autoConfirmed(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should validate 12-digit UTR and transition payment to AUTO_CONFIRMED without prematurely unlocking contact details")
    void createPaymentReference_Valid12DigitUtr_TransitionsToAutoConfirmed() {
        PaymentRequest request = new PaymentRequest(100L, "123456789012", "idemp-key-1");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByUpiReferenceId("123456789012")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.createPaymentReference(request, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTO_CONFIRMED);
        assertThat(response.isAutoConfirmed()).isTrue();
        assertThat(response.getUpiReferenceId()).isEqualTo("123456789012");
        // Booking must remain PENDING so contact info is locked until admin VERIFIES
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(auditLogRepository, times(1)).save(any(PaymentAuditLog.class));
        verify(emailService, times(1)).sendPaymentSubmittedNotification(any());
    }

    @Test
    @DisplayName("Should reject invalid UTR format (non-12-digit)")
    void createPaymentReference_InvalidUtr_ThrowsIllegalArgument() {
        PaymentRequest request = new PaymentRequest(100L, "short123");

        assertThatThrownBy(() -> paymentService.createPaymentReference(request, "customer@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12-digit UTR number");

        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("Should reject duplicate UTR reuse across different bookings")
    void createPaymentReference_DuplicateUtr_ThrowsIllegalState() {
        PaymentRequest request = new PaymentRequest(100L, "123456789012");

        Booking otherBooking = Booking.builder().id(999L).build();
        Payment existingPayment = Payment.builder().id(2L).booking(otherBooking).upiReferenceId("123456789012").build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByUpiReferenceId("123456789012")).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.createPaymentReference(request, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has already been used");
    }

    @Test
    @DisplayName("Should return existing payment when idempotency key is matched")
    void createPaymentReference_IdempotentKey_ReturnsExisting() {
        PaymentRequest request = new PaymentRequest(100L, "123456789012", "idemp-key-repeat");

        when(paymentRepository.findByIdempotencyKey("idemp-key-repeat")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.createPaymentReference(request, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin confirm transitions payment to VERIFIED and unlocks contact by confirming booking")
    void confirmPayment_TransitionsToVerified_ConfirmsBooking() {
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        request.setNotes("Bank UTR verified");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("admin@servenow.com")).thenReturn(Optional.of(admin));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.confirmPayment(1L, request, "admin@servenow.com");

        assertThat(response).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(auditLogRepository, times(1)).save(any(PaymentAuditLog.class));
        verify(emailService, times(1)).sendPaymentConfirmedNotification(any());
    }

    @Test
    @DisplayName("Admin reverse transitions payment to REVERSED and resets booking to PENDING")
    void reversePayment_TransitionsToReversed_ResetsBooking() {
        PaymentRejectRequest request = new PaymentRejectRequest();
        request.setNotes("Disputed reference");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("admin@servenow.com")).thenReturn(Optional.of(admin));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.reversePayment(1L, request, "admin@servenow.com");

        assertThat(response).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(auditLogRepository, times(1)).save(any(PaymentAuditLog.class));
        verify(emailService, times(1)).sendPaymentRejectedNotification(any());
    }

    @Test
    @DisplayName("Should block invalid state transitions on terminal states")
    void stateMachine_InvalidTransition_ThrowsIllegalState() {
        Payment reversedPayment = Payment.builder()
                .id(5L)
                .status(PaymentStatus.REVERSED)
                .booking(booking)
                .build();

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(reversedPayment));
        when(userRepository.findByEmail("admin@servenow.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> paymentService.confirmPayment(5L, null, "admin@servenow.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition payment from REVERSED to VERIFIED");
    }

    @Test
    @DisplayName("Should deny payment details access to unrelated customers")
    void getPaymentById_UnrelatedUser_ThrowsUnauthorized() {
        User otherCustomer = User.builder().id(99L).email("other@example.com").role(RoleType.CUSTOMER).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomer));

        assertThatThrownBy(() -> paymentService.getPaymentById(1L, "other@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }
}
