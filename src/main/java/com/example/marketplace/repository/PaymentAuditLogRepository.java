package com.example.marketplace.repository;

import com.example.marketplace.entity.PaymentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

    List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Page<PaymentAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    default void delete(PaymentAuditLog entity) {
        throw new UnsupportedOperationException("PaymentAuditLog records are immutable and cannot be deleted");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("PaymentAuditLog records are immutable and cannot be deleted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("PaymentAuditLog records are immutable and cannot be deleted");
    }
}
