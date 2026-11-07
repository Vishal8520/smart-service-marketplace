package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentConfirmRequest;
import com.example.marketplace.dto.request.PaymentRejectRequest;
import com.example.marketplace.dto.response.AdminAnalyticsResponse;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceListingRepository serviceListingRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ServiceListingService serviceListingService;

    @InjectMocks
    private AdminService adminService;

    private User provider;
    private ServiceListing serviceListing;

    @BeforeEach
    void setUp() {
        provider = User.builder()
                .id(10L)
                .name("Electrician Pro")
                .email("electrician@example.com")
                .role(RoleType.SERVICE_PROVIDER)
                .kycStatus(KycStatus.APPROVED)
                .build();

        serviceListing = ServiceListing.builder()
                .id(20L)
                .title("Wiring Repair")
                .provider(provider)
                .status(ServiceStatus.PENDING_REVIEW)
                .active(false)
                .build();
    }

    @Test
    @DisplayName("Successfully computes platform analytics")
    void getAnalytics_Success() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(RoleType.SERVICE_PROVIDER)).thenReturn(25L);
        when(userRepository.countByRole(RoleType.CUSTOMER)).thenReturn(75L);
        when(serviceListingRepository.count()).thenReturn(50L);
        when(bookingRepository.count()).thenReturn(200L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(150L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(30L);
        when(paymentRepository.calculateTotalPlatformRevenue()).thenReturn(new BigDecimal("250000.00"));
        when(reviewRepository.findAveragePlatformRating()).thenReturn(4.7);

        AdminAnalyticsResponse response = adminService.getAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalUsers()).isEqualTo(100L);
        assertThat(response.getTotalProviders()).isEqualTo(25L);
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("250000.00"));
        assertThat(response.getAveragePlatformRating()).isEqualTo(4.7);
    }

    @Test
    @DisplayName("Admin approves provider KYC")
    void approveProviderKyc_Success() {
        provider.setKycStatus(KycStatus.PENDING);
        when(userRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User approved = adminService.approveProviderKyc(10L);

        assertThat(approved.getKycStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(approved.getKycNotes()).contains("KYC Approved");
    }

    @Test
    @DisplayName("Admin rejects provider KYC")
    void rejectProviderKyc_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User rejected = adminService.rejectProviderKyc(10L, "Unclear government ID");

        assertThat(rejected.getKycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(rejected.getKycNotes()).isEqualTo("Unclear government ID");
    }

    @Test
    @DisplayName("Admin approves service listing for KYC-approved provider")
    void approveService_KycApproved_Success() {
        when(serviceListingRepository.findById(20L)).thenReturn(Optional.of(serviceListing));
        when(serviceListingRepository.save(any(ServiceListing.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceListingService.toResponse(any())).thenReturn(ServiceResponse.builder().id(20L).status(ServiceStatus.APPROVED).build());

        ServiceResponse response = adminService.approveService(20L);

        assertThat(response.getStatus()).isEqualTo(ServiceStatus.APPROVED);
        assertThat(serviceListing.isActive()).isTrue();
    }

    @Test
    @DisplayName("Admin approval rejected if provider KYC is not approved")
    void approveService_ProviderNotKycApproved_Throws() {
        provider.setKycStatus(KycStatus.PENDING);
        when(serviceListingRepository.findById(20L)).thenReturn(Optional.of(serviceListing));

        assertThatThrownBy(() -> adminService.approveService(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider Electrician Pro has not been KYC-approved yet");

        verify(serviceListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delegates confirm payment to paymentService")
    void confirmPayment_DelegatesSuccessfully() {
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        when(paymentService.confirmPayment(1L, request, "admin@servenow.com"))
                .thenReturn(PaymentResponse.builder().id(1L).status(PaymentStatus.VERIFIED).build());

        PaymentResponse response = adminService.confirmPayment(1L, request, "admin@servenow.com");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        verify(paymentService, times(1)).confirmPayment(1L, request, "admin@servenow.com");
    }

    @Test
    @DisplayName("Delegates reverse payment to paymentService")
    void reversePayment_DelegatesSuccessfully() {
        PaymentRejectRequest request = new PaymentRejectRequest();
        when(paymentService.reversePayment(1L, request, "admin@servenow.com"))
                .thenReturn(PaymentResponse.builder().id(1L).status(PaymentStatus.REVERSED).build());

        PaymentResponse response = adminService.reversePayment(1L, request, "admin@servenow.com");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        verify(paymentService, times(1)).reversePayment(1L, request, "admin@servenow.com");
    }
}
