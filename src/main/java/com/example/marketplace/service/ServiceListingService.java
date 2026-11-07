package com.example.marketplace.service;

import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.CityRepository;
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
    private final CityRepository cityRepo;

    public ServiceListingService(ServiceListingRepository serviceRepo, UserRepository userRepo,
                                 CityRepository cityRepo) {
        this.serviceRepo = serviceRepo;
        this.userRepo = userRepo;
        this.cityRepo = cityRepo;
    }

    @Transactional(readOnly = true)
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
        String emailToUse = (providerEmail != null && !providerEmail.isBlank())
                ? providerEmail
                : request.getProviderEmail();

        if (emailToUse == null || emailToUse.isBlank()) {
            emailToUse = "provider.default@marketplace.com";
        }

        final String finalEmail = emailToUse;
        User provider = userRepo.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .name("Service Provider")
                    .email(finalEmail)
                    .role(RoleType.SERVICE_PROVIDER)
                    .kycStatus(KycStatus.PENDING)
                    .build();
            return userRepo.save(newUser);
        });

        // Provider KYC verification check: only KYC-approved providers or admins can create listings
        if (provider.getRole() == RoleType.SERVICE_PROVIDER && provider.getKycStatus() != KycStatus.APPROVED) {
            throw new IllegalStateException("Provider account is not KYC-approved (status: "
                    + provider.getKycStatus() + "). Admin KYC approval is required before publishing service listings.");
        }

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
                .averageRating(0.0)
                .reviewCount(0)
                .build();

        return toResponse(serviceRepo.save(listing));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request, String providerEmail) {
        ServiceListing listing = getListingById(id);

        if (listing.getProvider() != null && listing.getProvider().getEmail() != null
                && !listing.getProvider().getEmail().equalsIgnoreCase(providerEmail)) {
            User user = userRepo.findByEmail(providerEmail).orElse(null);
            if (user == null || user.getRole() != RoleType.ADMIN) {
                throw new UnauthorizedException("You do not own this service listing");
            }
        }

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
        if (listing.getProvider() != null && listing.getProvider().getEmail() != null
                && !listing.getProvider().getEmail().equalsIgnoreCase(providerEmail)) {
            User user = userRepo.findByEmail(providerEmail).orElse(null);
            if (user == null || user.getRole() != RoleType.ADMIN) {
                throw new UnauthorizedException("You do not own this service listing");
            }
        }
        listing.setActive(false);
        serviceRepo.save(listing);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(Long id) {
        return toResponse(getListingById(id));
    }

    private ServiceListing getListingById(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
    }

    /**
     * Maps entity to response DTO without triggering N+1 query.
     * Uses precomputed averageRating stored directly on ServiceListing entity.
     */
    public ServiceResponse toResponse(ServiceListing s) {
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
                .averageRating(s.getAverageRating() != null ? s.getAverageRating() : 0.0)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
