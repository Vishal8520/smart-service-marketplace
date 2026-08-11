package com.example.marketplace.repository;

import com.example.marketplace.entity.ServiceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long> {

        @EntityGraph(attributePaths = { "category", "provider" })
        Page<ServiceListing> findByActiveTrue(Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider" })
        Page<ServiceListing> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

        @EntityGraph(attributePaths = { "category", "provider" })
        Page<ServiceListing> findByProviderIdAndActiveTrue(Long providerId, Pageable pageable);

        List<ServiceListing> findByProviderIdAndActiveTrue(Long providerId);

        @EntityGraph(attributePaths = { "category", "provider" })
        @Query("SELECT s FROM ServiceListing s WHERE s.active = true AND " +
                        "(:categoryId IS NULL OR s.category.id = :categoryId) AND " +
                        "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<ServiceListing> search(@Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        Pageable pageable);

        default Page<ServiceListing> search(String keyword, Long categoryId, String tag, Pageable pageable) {
                return search(keyword, categoryId, pageable);
        }
}
