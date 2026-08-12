package com.example.marketplace.repository;

import com.example.marketplace.entity.ServiceListing;
import com.example.marketplace.entity.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long> {

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findByActiveTrueAndStatus(ServiceStatus status, Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findByCategoryIdAndActiveTrueAndStatus(Long categoryId, ServiceStatus status,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findByCityIdAndActiveTrueAndStatus(Long cityId, ServiceStatus status, Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findByCityIdAndCategoryIdAndActiveTrueAndStatus(Long cityId, Long categoryId,
                        ServiceStatus status, Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findByProviderId(Long providerId, Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> findAllByStatus(ServiceStatus status, Pageable pageable);

        @Query("SELECT s FROM ServiceListing s WHERE s.active = true AND s.status = :status AND " +
                        "(LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Page<ServiceListing> searchActiveAndApprovedServices(@Param("query") String query,
                        @Param("status") ServiceStatus status, Pageable pageable);

        @Override
        @EntityGraph(attributePaths = { "category", "provider", "city", "tags" })
        Optional<ServiceListing> findById(Long id);
}
