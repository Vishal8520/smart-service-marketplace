package com.example.marketplace.controller;

import com.example.marketplace.dto.ai.*;
import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.service.ServiceListingService;
import com.example.marketplace.service.ai.AiServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Service listing CRUD and search endpoints")
public class ServiceController {

    private final ServiceListingService serviceListingService;
    private final AiServicePort aiServicePort;

    public ServiceController(ServiceListingService serviceListingService, AiServicePort aiServicePort) {
        this.serviceListingService = serviceListingService;
        this.aiServicePort = aiServicePort;
    }

    @GetMapping
    @Operation(summary = "Search active service listings with optional city/category/tag filters, sorting, and pagination")
    public ResponseEntity<Page<ServiceResponse>> searchServices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(serviceListingService.searchServices(keyword, categoryId, cityId, tag, pageable));
    }

    @GetMapping("/smart-search")
    @Operation(summary = "Smart semantic search powered by embeddings, FAISS & category intelligence")
    public ResponseEntity<SmartSearchResponseDto> smartSearch(
            @RequestParam String q,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") int limit) {
        SmartSearchResponseDto response = aiServicePort.smartSearch(q, city, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommendations")
    @Operation(summary = "Get multi-factor ranked recommendations with explainability rationale")
    public ResponseEntity<RecommendationResponseDto> getRecommendations(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String userCity,
            @RequestParam(defaultValue = "5") int limit) {
        // Fetch candidates from database
        Pageable pageable = PageRequest.of(0, 20, Sort.by("averageRating").descending());
        Page<ServiceResponse> candidatesPage = serviceListingService.searchServices(query, null, null, null, pageable);

        List<CandidateServiceDto> candidates = candidatesPage.getContent().stream()
                .map(s -> new CandidateServiceDto(
                        s.getId(),
                        s.getTitle(),
                        s.getCategoryName(),
                        s.getPrice() != null ? s.getPrice().doubleValue() : 0.0,
                        s.getAverageRating() != null ? s.getAverageRating() : 0.0,
                        s.getCityName(),
                        0.95,
                        0L
                ))
                .collect(Collectors.toList());

        RecommendationResponseDto recommendations = aiServicePort.getRecommendations(query, userCity, candidates, limit);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/listing-assist")
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "AI Listing Assistant: generate description, fair price range, and content compliance")
    public ResponseEntity<ListingAssistResponseDto> assistListing(
            @Valid @RequestBody ListingAssistRequestDto request) {
        ListingAssistResponseDto response = aiServicePort.assistListing(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a single service listing (Provider contact details hidden)")
    public ResponseEntity<ServiceResponse> getService(@PathVariable Long id) {
        return ResponseEntity.ok(serviceListingService.getService(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "Create a new service listing (Requires KYC approval, defaults to PENDING_REVIEW)")
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request,
            Authentication authentication) {
        String providerEmail = authentication != null ? authentication.getName() : request.getProviderEmail();
        if (providerEmail == null || providerEmail.isBlank()) {
            throw new UnauthorizedException("Provider authentication required");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceListingService.createService(request, providerEmail));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "Update an existing service listing (Ownership enforced)")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request,
            Authentication authentication) {
        String providerEmail = authentication != null ? authentication.getName() : request.getProviderEmail();
        if (providerEmail == null || providerEmail.isBlank()) {
            throw new UnauthorizedException("Provider authentication required");
        }
        return ResponseEntity.ok(serviceListingService.updateService(id, request, providerEmail));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "Deactivate a service listing (Ownership enforced)")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Provider authentication required");
        }
        serviceListingService.deleteService(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
