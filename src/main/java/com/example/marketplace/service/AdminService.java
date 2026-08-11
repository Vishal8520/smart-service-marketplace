package com.example.marketplace.service;

import com.example.marketplace.dto.response.AdminAnalyticsResponse;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ServiceListingRepository serviceListingRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    public AdminService(UserRepository userRepository, ServiceListingRepository serviceListingRepository,
            BookingRepository bookingRepository, ReviewRepository reviewRepository,
            PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.serviceListingRepository = serviceListingRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        long totalUsers = userRepository.count();
        long totalProviders = userRepository.countByRole(RoleType.SERVICE_PROVIDER);
        long totalCustomers = userRepository.countByRole(RoleType.CUSTOMER);

        long totalServices = serviceListingRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        BigDecimal totalRevenue = paymentRepository.calculateTotalPlatformRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Double avgRating = reviewRepository.findAveragePlatformRating();
        double averagePlatformRating = avgRating != null ? avgRating : 0.0;

        return AdminAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .totalProviders(totalProviders)
                .totalCustomers(totalCustomers)
                .totalServices(totalServices)
                .totalBookings(totalBookings)
                .pendingBookings(pendingBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue)
                .averagePlatformRating(averagePlatformRating)
                .build();
    }
}
