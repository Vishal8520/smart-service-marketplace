package com.example.marketplace.service;

import com.example.marketplace.dto.request.ReviewRequest;
import com.example.marketplace.dto.response.ReviewResponse;
import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.entity.Review;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.ReviewRepository;
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

    public ReviewService(ReviewRepository reviewRepo, BookingRepository bookingRepo, UserRepository userRepo) {
        this.reviewRepo = reviewRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
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
            throw new IllegalStateException("Reviews can only be submitted for completed bookings");
        }
        if (reviewRepo.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("A review already exists for this booking");
        }

        Review review = Review.builder()
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toResponse(reviewRepo.save(review));
    }

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
