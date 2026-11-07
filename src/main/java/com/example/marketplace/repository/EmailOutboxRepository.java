package com.example.marketplace.repository;

import com.example.marketplace.entity.EmailOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    List<EmailOutbox> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(String status, int maxAttempts, Pageable pageable);

    List<EmailOutbox> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
