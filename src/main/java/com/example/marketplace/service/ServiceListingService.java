package com.example.marketplace.service;

import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.entity.Category;
import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
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

    public ServiceListingService(ServiceListingRepository serviceRepo, UserRepository userRepo,
            ReviewRepository reviewRepo) {
        this.serviceRepo = serviceRepo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
    }

    public Page<ServiceResponse> searchServices(String keyword, Long categoryId, String tag, Pageable pageable) {
        Page<ServiceListing> page;
        if (keyword != null && !keyword.isBlank()) {
            page = serviceRepo.search(keyword, categoryId, tag, pageable);
        } else if (categoryId != null) {
            page = serviceRepo.findByCategoryIdAndActiveTrue(categoryId, pageable);
        } else {
            page = serviceRepo.findByActiveTrue(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request, String providerEmail) {
        User provider = getUserByEmail(providerEmail);

        Category category = new Category();
        category.setId(request.getCategoryId());

        ServiceListing listing = ServiceListing.builder()
                .provider(provider)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(request.getTags())
                .price(request.getPrice())
                .build();

        return toResponse(serviceRepo.save(listing));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request, String providerEmail) {
        ServiceListing listing = getListingById(id);
        validateOwnership(listing, providerEmail);

        Category category = new Category();
        category.setId(request.getCategoryId());

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
        validateOwnership(listing, providerEmail);
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

    private User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateOwnership(ServiceListing listing, String providerEmail) {
        if (!listing.getProvider().getEmail().equals(providerEmail)) {
            throw new UnauthorizedException("You do not own this service listing");
        }
    }

    public ServiceResponse toResponse(ServiceListing s) {
        Double avgRating = reviewRepo.averageRatingByServiceId(s.getId());
        return ServiceResponse.builder()
                .id(s.getId())
                .providerId(s.getProvider().getId())
                .providerName(s.getProvider().getName())
                .providerEmail(s.getProvider().getEmail())
                .categoryId(s.getCategory() != null ? s.getCategory().getId() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .title(s.getTitle())
                .description(s.getDescription())
                .tags(s.getTags())
                .price(s.getPrice())
                .active(s.isActive())
                .averageRating(avgRating)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
