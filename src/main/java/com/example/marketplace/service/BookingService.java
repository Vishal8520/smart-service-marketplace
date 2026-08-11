package com.example.marketplace.service;

import com.example.marketplace.dto.request.BookingRequest;
import com.example.marketplace.dto.request.BookingStatusRequest;
import com.example.marketplace.dto.response.BookingResponse;
import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.ServiceListingRepository;
import com.example.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ServiceListingRepository serviceRepo;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepo, UserRepository userRepo, ServiceListingRepository serviceRepo,
            NotificationService notificationService) {
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.serviceRepo = serviceRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String customerEmail) {
        User customer = getUserByEmail(customerEmail);
        ServiceListing service = serviceRepo.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

        if (!service.isActive()) {
            throw new IllegalStateException("Service is not currently active");
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .service(service)
                .scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .build();

        Booking saved = bookingRepo.save(booking);
        notificationService.sendBookingConfirmation(saved);
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
        User provider = getUserByEmail(providerEmail);

        if (!booking.getService().getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not the provider for this booking");
        }

        validateStatusTransition(booking.getStatus(), request.getStatus());
        booking.setStatus(request.getStatus());
        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String customerEmail) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getCustomer().getEmail().equals(customerEmail)) {
            throw new UnauthorizedException("You cannot cancel this booking");
        }
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
            throw new IllegalStateException(
                    "Cannot transition booking from " + current + " to " + next);
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

    public BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .customerId(b.getCustomer().getId())
                .customerName(b.getCustomer().getName())
                .serviceId(b.getService().getId())
                .serviceTitle(b.getService().getTitle())
                .price(b.getService().getPrice())
                .providerId(b.getService().getProvider().getId())
                .providerName(b.getService().getProvider().getName())
                .scheduledAt(b.getScheduledAt())
                .status(b.getStatus())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
