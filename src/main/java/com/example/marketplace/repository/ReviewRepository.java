package com.example.marketplace.repository;

import com.example.marketplace.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT r FROM Review r WHERE r.booking.service.id = :serviceId")
    Page<Review> findByServiceId(@Param("serviceId") Long serviceId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.service.id = :serviceId")
    Double averageRatingByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.service.provider.id = :providerId")
    Double averageRatingByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double findAveragePlatformRating();
}
