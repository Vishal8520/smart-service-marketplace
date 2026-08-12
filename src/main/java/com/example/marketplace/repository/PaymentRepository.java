package com.example.marketplace.repository;

import com.example.marketplace.entity.Payment;
import com.example.marketplace.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = { "booking", "booking.customer", "booking.service", "booking.service.provider",
            "confirmedByAdmin", "reversedByAdmin" })
    Optional<Payment> findByBookingId(Long bookingId);

    @EntityGraph(attributePaths = { "booking", "booking.customer", "booking.service", "booking.service.provider",
            "confirmedByAdmin", "reversedByAdmin" })
    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "booking", "booking.customer", "booking.service", "booking.service.provider",
            "confirmedByAdmin", "reversedByAdmin" })
    Page<Payment> findAll(Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'CONFIRMED' OR p.status = 'AUTO_CONFIRMED'")
    BigDecimal calculateTotalPlatformRevenue();
}
