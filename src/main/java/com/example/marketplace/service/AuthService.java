package com.example.marketplace.service;

import com.example.marketplace.dto.request.LoginRequest;
import com.example.marketplace.dto.request.RefreshTokenRequest;
import com.example.marketplace.dto.request.RegisterRequest;
import com.example.marketplace.dto.response.AuthResponse;
import com.example.marketplace.entity.KycStatus;
import com.example.marketplace.entity.RefreshToken;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.RateLimitExceededException;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.RefreshTokenRepository;
import com.example.marketplace.repository.UserRepository;
import com.example.marketplace.security.JwtService;
import com.example.marketplace.security.RateLimitingService;
import com.example.marketplace.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final RateLimitingService rateLimitingService;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService,
                        AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService,
                        RateLimitingService rateLimitingService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.rateLimitingService = rateLimitingService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        RoleType role = RoleType.valueOf(request.getRole());
        // Service providers start with PENDING KYC approval; Customers start APPROVED
        KycStatus initialKyc = (role == RoleType.SERVICE_PROVIDER) ? KycStatus.PENDING : KycStatus.APPROVED;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .kycStatus(initialKyc)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        return createAuthSession(user, userDetails);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Rate Limiting via Bucket4j
        if (!rateLimitingService.tryConsumeLogin(email)) {
            throw new RateLimitExceededException("Too many login attempts for " + email + ". Please wait 1 minute before retrying.");
        }

        // 2. Check Account Lockout
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isAccountNonLocked()) {
                throw new LockedException("Account is temporarily locked due to multiple failed login attempts. Try again later.");
            }
        }

        // 3. Attempt Authentication
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setAccountLockedUntil(Instant.now().plus(LOCK_DURATION));
                    log.warn("Account {} locked until {} after {} consecutive failed login attempts",
                            email, user.getAccountLockedUntil(), attempts);
                }
                userRepository.save(user);
            }
            throw ex;
        }

        User user = userOpt.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 4. Reset failed attempts upon successful login
        if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return createAuthSession(user, userDetails);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        String email = jwtService.extractUsername(rawToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(rawToken, userDetails)) {
            throw new IllegalArgumentException("Invalid or expired refresh token signature");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found in active session registry"));

        // Refresh token rotation & reuse detection (RFC 6749 security BCP)
        if (storedToken.isRevoked()) {
            log.warn("Revoked refresh token reuse detected for user {}! Invalidating all active sessions.", email);
            refreshTokenRepository.revokeAllUserTokens(user.getId());
            throw new IllegalArgumentException("Security violation: Revoked refresh token reuse detected. All sessions terminated.");
        }

        if (storedToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        // Revoke current token upon rotation
        storedToken.setRevoked(true);

        // Issue new access and refresh token pair
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        String newTokenHash = hashToken(newRefreshToken);

        storedToken.setReplacedByToken(newTokenHash);
        refreshTokenRepository.save(storedToken);

        // Save new refresh token
        RefreshToken newTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(newTokenEntity);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            String tokenHash = hashToken(request.getRefreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    private AuthResponse createAuthSession(User user, UserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String rawRefreshToken = jwtService.generateRefreshToken(userDetails);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
