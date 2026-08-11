package com.example.marketplace.repository;

import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = { "customer", "service", "service.category", "service.provider" })
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "service", "service.category", "service.provider" })
    @Query("SELECT b FROM Booking b WHERE b.service.provider.id = :providerId ORDER BY b.createdAt DESC")
    Page<Booking> findByServiceProviderIdOrderByCreatedAtDesc(@Param("providerId") Long providerId, Pageable pageable);

    List<Booking> findByCustomerIdAndStatus(Long customerId, BookingStatus status);

    boolean existsByCustomerIdAndServiceIdAndStatus(Long customerId, Long serviceId, BookingStatus status);

    // For admin analytics
    long countByStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :from AND b.createdAt <= :to")
    long countBookingsInRange(@Param("from") Instant from, @Param("to") Instant to);
}
