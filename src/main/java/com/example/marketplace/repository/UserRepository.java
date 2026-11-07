package com.example.marketplace.repository;

import com.example.marketplace.entity.KycStatus;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(RoleType role);

    Page<User> findByRoleAndKycStatus(RoleType role, KycStatus kycStatus, Pageable pageable);

    long countByRoleAndKycStatus(RoleType role, KycStatus kycStatus);
}
