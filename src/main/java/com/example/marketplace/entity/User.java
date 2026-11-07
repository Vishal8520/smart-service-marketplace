package com.example.marketplace.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleType role;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.APPROVED;

    @Column(name = "kyc_notes", columnDefinition = "TEXT")
    private String kycNotes;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User() {
    }

    public User(Long id, String name, String email, String password, String phone, RoleType role,
                KycStatus kycStatus, String kycNotes, int failedLoginAttempts, Instant accountLockedUntil, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.kycStatus = kycStatus != null ? kycStatus : KycStatus.APPROVED;
        this.kycNotes = kycNotes;
        this.failedLoginAttempts = failedLoginAttempts;
        this.accountLockedUntil = accountLockedUntil;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public User(Long id, String name, String email, String password, String phone, RoleType role, Instant createdAt) {
        this(id, name, email, password, phone, role, KycStatus.APPROVED, null, 0, null, createdAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String password;
        private String phone;
        private RoleType role;
        private KycStatus kycStatus = KycStatus.APPROVED;
        private String kycNotes;
        private int failedLoginAttempts = 0;
        private Instant accountLockedUntil;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder role(RoleType role) {
            this.role = role;
            return this;
        }

        public Builder kycStatus(KycStatus kycStatus) {
            this.kycStatus = kycStatus;
            return this;
        }

        public Builder kycNotes(String kycNotes) {
            this.kycNotes = kycNotes;
            return this;
        }

        public Builder failedLoginAttempts(int failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder accountLockedUntil(Instant accountLockedUntil) {
            this.accountLockedUntil = accountLockedUntil;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(id, name, email, password, phone, role, kycStatus, kycNotes,
                    failedLoginAttempts, accountLockedUntil, createdAt);
        }
    }

    public boolean isAccountNonLocked() {
        if (accountLockedUntil == null) {
            return true;
        }
        return Instant.now().isAfter(accountLockedUntil);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getKycNotes() {
        return kycNotes;
    }

    public void setKycNotes(String kycNotes) {
        this.kycNotes = kycNotes;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getAccountLockedUntil() {
        return accountLockedUntil;
    }

    public void setAccountLockedUntil(Instant accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
