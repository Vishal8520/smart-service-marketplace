package com.example.marketplace.repository;

import com.example.marketplace.entity.PaymentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

    List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Page<PaymentAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
