package com.example.marketplace.service;

import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.Category;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.UnauthorizedException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceListingServiceTest {

    @Mock
    private ServiceListingRepository serviceRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ReviewRepository reviewRepo;

    @InjectMocks
    private ServiceListingService serviceListingService;

    private User provider;
    private User otherUser;
    private Category category;
    private ServiceListing listing;
    private ServiceRequest serviceRequest;

    @BeforeEach
    void setUp() {
        provider = User.builder()
                .id(1L)
                .email("provider@example.com")
                .name("Bob Plumbing")
                .role(RoleType.SERVICE_PROVIDER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .role(RoleType.SERVICE_PROVIDER)
                .build();

        category = Category.builder().id(1L).name("Plumbing").build();

        listing = ServiceListing.builder()
                .id(10L)
                .provider(provider)
                .category(category)
                .title("Pipe Leak Repair")
                .description("Fixing leaky pipes quickly")
                .price(BigDecimal.valueOf(499.00))
                .tags(List.of("plumbing", "repair"))
                .active(true)
                .build();

        serviceRequest = new ServiceRequest();
        serviceRequest.setTitle("Pipe Leak Repair");
        serviceRequest.setDescription("Fixing leaky pipes quickly");
        serviceRequest.setCategoryId(1L);
        serviceRequest.setPrice(BigDecimal.valueOf(499.00));
        serviceRequest.setTags(List.of("plumbing", "repair"));
    }

    @Test
    @DisplayName("Provider can create a new service listing")
    void createService_Success() {
        when(userRepo.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        when(serviceRepo.save(any())).thenReturn(listing);
        when(reviewRepo.averageRatingByServiceId(10L)).thenReturn(4.8);

        ServiceResponse response = serviceListingService.createService(serviceRequest, provider.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Pipe Leak Repair");
        assertThat(response.getAverageRating()).isEqualTo(4.8);
        verify(serviceRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("Updating someone else's service listing throws UnauthorizedException")
    void updateService_Unauthorized_ThrowsException() {
        when(serviceRepo.findById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> serviceListingService.updateService(10L, serviceRequest, otherUser.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You do not own this service listing");
    }
}
