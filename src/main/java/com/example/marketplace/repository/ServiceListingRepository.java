package com.example.marketplace.repository;

import com.example.marketplace.entity.ServiceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long> {

        Page<ServiceListing> findByActiveTrue(Pageable pageable);

        Page<ServiceListing> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

        Page<ServiceListing> findByProviderIdAndActiveTrue(Long providerId, Pageable pageable);

        List<ServiceListing> findByProviderIdAndActiveTrue(Long providerId);

        @Query(value = "SELECT * FROM service_listings s WHERE s.active = true AND " +
                        "(:categoryId IS NULL OR s.category_id = :categoryId) AND " +
                        "(:tag IS NULL OR :tag = ANY(s.tags)) AND " +
                        "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))", countQuery = "SELECT COUNT(*) FROM service_listings s WHERE s.active = true AND "
                                        +
                                        "(:categoryId IS NULL OR s.category_id = :categoryId) AND " +
                                        "(:tag IS NULL OR :tag = ANY(s.tags)) AND " +
                                        "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                                        +
                                        " LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))", nativeQuery = true)
        Page<ServiceListing> search(@Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("tag") String tag,
                        Pageable pageable);
}
