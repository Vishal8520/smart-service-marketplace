package com.example.marketplace.service;

import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.CityRepository;
import com.example.marketplace.repository.ReviewRepository;
import com.example.marketplace.repository.ServiceListingRepository;
import com.example.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceListingService {

    private final ServiceListingRepository serviceRepo;
    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final CityRepository cityRepo;

    public ServiceListingService(ServiceListingRepository serviceRepo, UserRepository userRepo,
            ReviewRepository reviewRepo, CityRepository cityRepo) {
        this.serviceRepo = serviceRepo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.cityRepo = cityRepo;
    }

    public Page<ServiceResponse> searchServices(String keyword, Long categoryId, Long cityId, String tag,
            Pageable pageable) {
        Page<ServiceListing> page;
        if (keyword != null && !keyword.isBlank()) {
            page = serviceRepo.searchActiveAndApprovedServices(keyword, ServiceStatus.APPROVED, pageable);
        } else if (cityId != null && categoryId != null) {
            page = serviceRepo.findByCityIdAndCategoryIdAndActiveTrueAndStatus(cityId, categoryId,
                    ServiceStatus.APPROVED, pageable);
        } else if (cityId != null) {
            page = serviceRepo.findByCityIdAndActiveTrueAndStatus(cityId, ServiceStatus.APPROVED, pageable);
        } else if (categoryId != null) {
            page = serviceRepo.findByCategoryIdAndActiveTrueAndStatus(categoryId, ServiceStatus.APPROVED, pageable);
        } else {
            page = serviceRepo.findByActiveTrueAndStatus(ServiceStatus.APPROVED, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request, String providerEmail) {
        String emailToUse = (request.getProviderEmail() != null && !request.getProviderEmail().isBlank())
                ? request.getProviderEmail()
                : providerEmail;

        if (emailToUse == null || emailToUse.isBlank()) {
            emailToUse = "provider.default@marketplace.com";
        }

        final String finalEmail = emailToUse;
        User provider = userRepo.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .name("Service Provider")
                    .email(finalEmail)
                    .role(RoleType.SERVICE_PROVIDER)
                    .build();
            return userRepo.save(newUser);
        });

        Category category = new Category();
        category.setId(request.getCategoryId());

        City city = null;
        if (request.getCityId() != null) {
            city = cityRepo.findById(request.getCityId()).orElse(null);
        }

        // New listings default to PENDING_REVIEW unless created by admin
        ServiceStatus initialStatus = (provider.getRole() == RoleType.ADMIN) ? ServiceStatus.APPROVED
                : ServiceStatus.PENDING_REVIEW;

        ServiceListing listing = ServiceListing.builder()
                .provider(provider)
                .category(category)
                .city(city)
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(request.getTags())
                .price(request.getPrice())
                .status(initialStatus)
                .active(initialStatus == ServiceStatus.APPROVED)
                .build();

        return toResponse(serviceRepo.save(listing));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request, String providerEmail) {
        ServiceListing listing = getListingById(id);

        Category category = new Category();
        category.setId(request.getCategoryId());

        if (request.getCityId() != null) {
            City city = cityRepo.findById(request.getCityId()).orElse(null);
            listing.setCity(city);
        }

        listing.setCategory(category);
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setTags(request.getTags());
        listing.setPrice(request.getPrice());

        return toResponse(serviceRepo.save(listing));
    }

    @Transactional
    public void deleteService(Long id, String providerEmail) {
        ServiceListing listing = getListingById(id);
        listing.setActive(false);
        serviceRepo.save(listing);
    }

    public ServiceResponse getService(Long id) {
        return toResponse(getListingById(id));
    }

    private ServiceListing getListingById(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
    }

    public ServiceResponse toResponse(ServiceListing s) {
        Double avgRating = reviewRepo.averageRatingByServiceId(s.getId());
        return ServiceResponse.builder()
                .id(s.getId())
                .providerId(s.getProvider() != null ? s.getProvider().getId() : null)
                .providerName(s.getProvider() != null ? s.getProvider().getName() : "Professional Provider")
                .providerEmail(null) // SERVER-SIDE CONTACT HIDING: Do NOT expose email/phone during service browsing
                .categoryId(s.getCategory() != null ? s.getCategory().getId() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .cityId(s.getCity() != null ? s.getCity().getId() : null)
                .cityName(s.getCity() != null ? s.getCity().getName() : "All Cities")
                .status(s.getStatus())
                .title(s.getTitle())
                .description(s.getDescription())
                .tags(s.getTags())
                .price(s.getPrice())
                .active(s.isActive())
                .averageRating(avgRating != null ? avgRating : 4.8)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
