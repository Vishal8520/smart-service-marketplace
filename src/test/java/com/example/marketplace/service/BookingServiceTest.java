package com.example.marketplace.service;

import com.example.marketplace.dto.request.BookingRequest;
import com.example.marketplace.dto.request.BookingStatusRequest;
import com.example.marketplace.dto.response.BookingResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.PaymentRepository;
import com.example.marketplace.repository.ServiceListingRepository;
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
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ServiceListingRepository serviceRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private User provider;
    private ServiceListing service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("alice@example.com")
                .name("Alice Customer")
                .phone("+919876543210")
                .role(RoleType.CUSTOMER)
                .build();

        provider = User.builder()
                .id(2L)
                .email("bob@example.com")
                .name("Bob Expert")
                .phone("+919123456789")
                .role(RoleType.SERVICE_PROVIDER)
                .build();

        service = ServiceListing.builder()
                .id(10L)
                .title("AC Service")
                .price(BigDecimal.valueOf(999.00))
                .provider(provider)
                .active(true)
                .build();

        booking = Booking.builder()
                .id(100L)
                .customer(customer)
                .service(service)
                .status(BookingStatus.PENDING)
                .scheduledAt(Instant.now().plusSeconds(86400))
                .address("123 Elm St")
                .build();
    }

    @Test
    @DisplayName("Successfully creates booking and pre-creates pending payment")
    void createBooking_Success() {
        BookingRequest request = new BookingRequest(10L, Instant.now().plusSeconds(86400),
                "Alice Customer", "alice@example.com", "+919876543210", "123 Elm St", "Ring bell");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(serviceRepo.findById(10L)).thenReturn(Optional.of(service));
        when(bookingRepo.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.createBooking(request, "alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        // Contact details MUST be locked (null) for PENDING status
        assertThat(response.getProviderPhone()).isNull();
        assertThat(response.getProviderEmail()).isNull();

        verify(paymentRepo, times(1)).save(any(Payment.class));
        verify(emailService, times(1)).sendBookingSubmittedNotification(any());
    }

    @Test
    @DisplayName("Throws IllegalStateException when booking an inactive service")
    void createBooking_InactiveService_Throws() {
        service.setActive(false);
        BookingRequest request = new BookingRequest(10L, Instant.now(), "Alice", "alice@example.com", null, null, null);

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(serviceRepo.findById(10L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> bookingService.createBooking(request, "alice@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not currently active");
    }

    @Test
    @DisplayName("Provider updates booking status from PENDING to CONFIRMED")
    void updateStatus_ProviderOwnership_Success() {
        BookingStatusRequest request = new BookingStatusRequest(BookingStatus.CONFIRMED);

        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepo.findByEmail("bob@example.com")).thenReturn(Optional.of(provider));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.updateStatus(100L, request, "bob@example.com");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        // Once CONFIRMED, provider contact info is unlocked!
        assertThat(response.getProviderPhone()).isEqualTo("+919123456789");
        assertThat(response.getProviderEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("Unrelated provider cannot update booking status")
    void updateStatus_UnauthorizedProvider_Throws() {
        User otherProvider = User.builder().id(99L).email("other@example.com").role(RoleType.SERVICE_PROVIDER).build();
        BookingStatusRequest request = new BookingStatusRequest(BookingStatus.CONFIRMED);

        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepo.findByEmail("other@example.com")).thenReturn(Optional.of(otherProvider));

        assertThatThrownBy(() -> bookingService.updateStatus(100L, request, "other@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only update bookings for your own services");
    }

    @Test
    @DisplayName("Customer can cancel their pending booking")
    void cancelBooking_CustomerOwnership_Success() {
        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(100L, "alice@example.com");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Different customer cannot cancel booking")
    void cancelBooking_UnauthorizedCustomer_Throws() {
        User impostor = User.builder().id(99L).email("impostor@example.com").role(RoleType.CUSTOMER).build();

        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepo.findByEmail("impostor@example.com")).thenReturn(Optional.of(impostor));

        assertThatThrownBy(() -> bookingService.cancelBooking(100L, "impostor@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only access your own bookings");
    }

    @Test
    @DisplayName("Cannot cancel an already completed booking")
    void cancelBooking_AlreadyCompleted_Throws() {
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepo.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> bookingService.cancelBooking(100L, "alice@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a COMPLETED booking");
    }
}
