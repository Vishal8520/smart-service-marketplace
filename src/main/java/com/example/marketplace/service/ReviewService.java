package com.example.marketplace.service;

import com.example.marketplace.dto.request.ReviewRequest;
import com.example.marketplace.dto.response.ReviewResponse;
import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.entity.Review;
import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.ReviewRepository;
import com.example.marketplace.repository.ServiceListingRepository;
import com.example.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ServiceListingRepository serviceRepo;

    public ReviewService(ReviewRepository reviewRepo, BookingRepository bookingRepo, UserRepository userRepo,
                         ServiceListingRepository serviceRepo) {
        this.reviewRepo = reviewRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.serviceRepo = serviceRepo;
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, String customerEmail) {
        User customer = getUserByEmail(customerEmail);

        Booking booking = bookingRepo.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedException("You can only review your own bookings");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted for completed bookings (current status: "
                    + booking.getStatus() + ")");
        }
        if (reviewRepo.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("A review already exists for this booking");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = Review.builder()
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepo.save(review);

        // Transactionally recompute average rating & review count for the service listing
        ServiceListing service = booking.getService();
        if (service != null) {
            Double newAvg = reviewRepo.averageRatingByServiceId(service.getId());
            long count = reviewRepo.countByServiceId(service.getId());
            service.setAverageRating(newAvg != null ? Math.round(newAvg * 10.0) / 10.0 : 0.0);
            service.setReviewCount((int) count);
            serviceRepo.save(service);
        }

        return toResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForService(Long serviceId, Pageable pageable) {
        return reviewRepo.findByServiceId(serviceId, pageable).map(this::toResponse);
    }

    private User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .customerId(r.getBooking().getCustomer().getId())
                .customerName(r.getBooking().getCustomer().getName())
                .serviceId(r.getBooking().getService().getId())
                .serviceTitle(r.getBooking().getService().getTitle())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
