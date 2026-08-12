package com.example.marketplace.controller;

import com.example.marketplace.dto.request.ServiceRequest;
import com.example.marketplace.dto.response.ServiceResponse;
import com.example.marketplace.service.ServiceListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Service listing CRUD and search endpoints")
public class ServiceController {

    private final ServiceListingService serviceListingService;

    public ServiceController(ServiceListingService serviceListingService) {
        this.serviceListingService = serviceListingService;
    }

    @GetMapping
    @Operation(summary = "Search active service listings with optional city/category/tag filters and pagination")
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

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a single service listing (Provider contact details hidden)")
    public ResponseEntity<ServiceResponse> getService(@PathVariable Long id) {
        return ResponseEntity.ok(serviceListingService.getService(id));
    }

    @PostMapping
    @Operation(summary = "Create a new service listing (Passwordless Provider Flow, defaults to PENDING_REVIEW)")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceListingService.createService(request, request.getProviderEmail()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing service listing")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceListingService.updateService(id, request, request.getProviderEmail()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a service listing")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @RequestParam(required = false) String providerEmail) {
        serviceListingService.deleteService(id, providerEmail);
        return ResponseEntity.noContent().build();
    }
}
