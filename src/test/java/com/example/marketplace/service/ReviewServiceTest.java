package com.example.marketplace.service;

import com.example.marketplace.dto.request.ReviewRequest;
import com.example.marketplace.dto.response.ReviewResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.ReviewRepository;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ServiceListingRepository serviceRepo;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private User provider;
    private ServiceListing service;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(1L).email("customer@example.com").name("Customer").role(RoleType.CUSTOMER).build();
        provider = User.builder().id(2L).email("provider@example.com").name("Provider").role(RoleType.SERVICE_PROVIDER).build();

        service = ServiceListing.builder()
                .id(10L)
                .title("Home Painting")
                .price(BigDecimal.valueOf(5000.00))
                .provider(provider)
                .averageRating(0.0)
                .reviewCount(0)
                .build();

        completedBooking = Booking.builder()
                .id(50L)
                .customer(customer)
                .service(service)
                .status(BookingStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Successfully creates review for completed booking and recalculates average rating")
    void createReview_CompletedBooking_Success() {
        ReviewRequest request = new ReviewRequest(50L, 5, "Outstanding service, very clean work!");

        Review savedReview = Review.builder()
                .id(1L)
                .booking(completedBooking)
                .rating(5)
                .comment("Outstanding service, very clean work!")
                .createdAt(Instant.now())
                .build();

        when(userRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepo.findById(50L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepo.existsByBookingId(50L)).thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepo.averageRatingByServiceId(10L)).thenReturn(4.8);
        when(reviewRepo.countByServiceId(10L)).thenReturn(5L);

        ReviewResponse response = reviewService.createReview(request, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Outstanding service, very clean work!");
        assertThat(service.getAverageRating()).isEqualTo(4.8);
        assertThat(service.getReviewCount()).isEqualTo(5);

        verify(serviceRepo, times(1)).save(service);
    }

    @Test
    @DisplayName("Throws IllegalStateException when trying to review a non-completed booking")
    void createReview_NonCompletedBooking_Throws() {
        completedBooking.setStatus(BookingStatus.CONFIRMED);
        ReviewRequest request = new ReviewRequest(50L, 5, "Too early!");

        when(userRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepo.findById(50L)).thenReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> reviewService.createReview(request, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reviews can only be submitted for completed bookings");
    }

    @Test
    @DisplayName("Throws IllegalStateException when duplicate review is attempted on same booking")
    void createReview_DuplicateReview_Throws() {
        ReviewRequest request = new ReviewRequest(50L, 5, "Second review!");

        when(userRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepo.findById(50L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepo.existsByBookingId(50L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request, "customer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A review already exists for this booking");
    }

    @Test
    @DisplayName("Throws UnauthorizedException when reviewing another customer's booking")
    void createReview_UnauthorizedCustomer_Throws() {
        User otherCustomer = User.builder().id(99L).email("other@example.com").build();
        ReviewRequest request = new ReviewRequest(50L, 4, "Not my booking");

        when(userRepo.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomer));
        when(bookingRepo.findById(50L)).thenReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> reviewService.createReview(request, "other@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only review your own bookings");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when rating is outside 1-5")
    void createReview_InvalidRating_Throws() {
        ReviewRequest request = new ReviewRequest(50L, 6, "Out of bounds rating");

        when(userRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepo.findById(50L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepo.existsByBookingId(50L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(request, "customer@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }
}
