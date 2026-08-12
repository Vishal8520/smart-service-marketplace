package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.response.AdminAnalyticsResponse;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.BookingStatus;
import com.example.marketplace.entity.PaymentStatus;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.ServiceStatus;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final PaymentService paymentService;
    private final ServiceListingService serviceListingService;

    public AdminService(UserRepository userRepository, ServiceListingRepository serviceListingRepository,
            BookingRepository bookingRepository, ReviewRepository reviewRepository,
            PaymentRepository paymentRepository, PaymentService paymentService,
            ServiceListingService serviceListingService) {
        this.userRepository = userRepository;
        this.serviceListingRepository = serviceListingRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.serviceListingService = serviceListingService;
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

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServicesByStatus(ServiceStatus status, Pageable pageable) {
        return serviceListingRepository.findAllByStatus(status, pageable)
                .map(serviceListingService::toResponse);
    }

    @Transactional
    public ServiceResponse approveService(Long id) {
        ServiceListing service = getServiceById(id);
        service.setStatus(ServiceStatus.APPROVED);
        service.setActive(true);
        return serviceListingService.toResponse(serviceListingRepository.save(service));
    }

    @Transactional
    public ServiceResponse rejectService(Long id) {
        ServiceListing service = getServiceById(id);
        service.setStatus(ServiceStatus.REJECTED);
        service.setActive(false);
        return serviceListingService.toResponse(serviceListingRepository.save(service));
    }

    @Transactional
    public ServiceResponse toggleServiceActive(Long id) {
        ServiceListing service = getServiceById(id);
        service.setActive(!service.isActive());
        return serviceListingService.toResponse(serviceListingRepository.save(service));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentService.getPaymentsByStatus(status, pageable);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
        return paymentService.confirmPayment(paymentId, request, adminEmail);
    }

    @Transactional
    public PaymentResponse reversePayment(Long paymentId, PaymentRejectRequest request, String adminEmail) {
        return paymentService.reversePayment(paymentId, request, adminEmail);
    }

    @Transactional
    public PaymentResponse rejectPayment(Long paymentId, PaymentRejectRequest request) {
        return paymentService.rejectPayment(paymentId, request);
    }

    private ServiceListing getServiceById(Long id) {
        return serviceListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceListing", "id", id));
    }
}
