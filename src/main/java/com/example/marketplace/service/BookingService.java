package com.example.marketplace.service;

import com.example.marketplace.dto.request.BookingRequest;
import com.example.marketplace.dto.request.BookingStatusRequest;
import com.example.marketplace.dto.response.BookingResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.PaymentRepository;
import com.example.marketplace.repository.ServiceListingRepository;
import com.example.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ServiceListingRepository serviceRepo;
    private final PaymentRepository paymentRepo;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepo, UserRepository userRepo, ServiceListingRepository serviceRepo,
            PaymentRepository paymentRepo, NotificationService notificationService) {
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.serviceRepo = serviceRepo;
        this.paymentRepo = paymentRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String authenticatedEmail) {
        String emailToUse = (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank())
                ? request.getCustomerEmail()
                : authenticatedEmail;

        if (emailToUse == null || emailToUse.isBlank()) {
            emailToUse = "customer.guest@marketplace.com";
        }

        final String finalEmail = emailToUse;
        User customer = userRepo.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .name(request.getCustomerName() != null ? request.getCustomerName() : "Valued Customer")
                    .email(finalEmail)
                    .phone(request.getCustomerPhone())
                    .role(RoleType.CUSTOMER)
                    .build();
            return userRepo.save(newUser);
        });

        ServiceListing service = serviceRepo.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

        if (!service.isActive()) {
            throw new IllegalStateException("Service is not currently active");
        }

        Instant scheduledAt = request.getScheduledAt() != null ? request.getScheduledAt()
                : Instant.now().plusSeconds(86400);

        Booking booking = Booking.builder()
                .customer(customer)
                .service(service)
                .scheduledAt(scheduledAt)
                .address(request.getAddress())
                .notes(request.getNotes())
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepo.save(booking);
        notificationService.sendBookingConfirmation(saved);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse payBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepo.save(booking);

        // Record simulated payment
        Payment payment = Payment.builder()
                .booking(saved)
                .amount(saved.getService().getPrice())
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .gatewayOrderId("PAY_MOCK_" + System.currentTimeMillis())
                .gatewayPaymentId("PAY_TXN_" + System.currentTimeMillis())
                .build();
        paymentRepo.save(payment);

        return toResponse(saved);
    }

    public Page<BookingResponse> getCustomerBookings(String customerEmail, Pageable pageable) {
        User customer = getUserByEmail(customerEmail);
        return bookingRepo.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::toResponse);
    }

    public Page<BookingResponse> getProviderBookings(String providerEmail, Pageable pageable) {
        User provider = getUserByEmail(providerEmail);
        return bookingRepo.findByServiceProviderIdOrderByCreatedAtDesc(provider.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatusRequest request, String providerEmail) {
        Booking booking = getBookingById(bookingId);
        validateStatusTransition(booking.getStatus(), request.getStatus());
        booking.setStatus(request.getStatus());
        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String customerEmail) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a " + booking.getStatus() + " booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepo.save(booking));
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next) {
        Set<BookingStatus> allowedFromPending = Set.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
        Set<BookingStatus> allowedFromConfirmed = Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED);

        boolean valid = switch (current) {
            case PENDING -> allowedFromPending.contains(next);
            case CONFIRMED -> allowedFromConfirmed.contains(next);
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Cannot transition booking from " + current + " to " + next);
        }
    }

    private Booking getBookingById(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }

    private User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * SERVER-SIDE CONTACT HIDING LOGIC:
     * Provider phone & email are ONLY populated if booking status is CONFIRMED or
     * COMPLETED (i.e. paid).
     * For PENDING bookings, phone & email remain NULL in the JSON payload returned
     * to clients.
     */
    public BookingResponse toResponse(Booking b) {
        boolean isPaid = (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED);

        User provider = b.getService() != null ? b.getService().getProvider() : null;
        String providerPhone = (isPaid && provider != null) ? provider.getPhone() : null;
        String providerEmail = (isPaid && provider != null) ? provider.getEmail() : null;

        User customer = b.getCustomer();

        return BookingResponse.builder()
                .id(b.getId())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : "Customer")
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .serviceId(b.getService() != null ? b.getService().getId() : null)
                .serviceTitle(b.getService() != null ? b.getService().getTitle() : "Service")
                .price(b.getService() != null ? b.getService().getPrice() : null)
                .providerId(provider != null ? provider.getId() : null)
                .providerName(provider != null ? provider.getName() : "Service Provider")
                .providerPhone(providerPhone)
                .providerEmail(providerEmail)
                .address(b.getAddress())
                .scheduledAt(b.getScheduledAt())
                .status(b.getStatus())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
