package com.example.marketplace.service;

import com.example.marketplace.dto.request.LoginRequest;
import com.example.marketplace.dto.request.RegisterRequest;
import com.example.marketplace.dto.response.AuthResponse;
import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.User;
import com.example.marketplace.repository.UserRepository;
import com.example.marketplace.security.JwtService;
import com.example.marketplace.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Provider");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole("SERVICE_PROVIDER");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id(1L)
                .name("Jane Provider")
                .email("jane@example.com")
                .password("hashed_password")
                .role(RoleType.SERVICE_PROVIDER)
                .build();

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com")
                .password("hashed_password")
                .authorities("ROLE_SERVICE_PROVIDER")
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(user);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access_jwt");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_jwt");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_jwt");
        assertThat(response.getRole()).isEqualTo("SERVICE_PROVIDER");
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate email")
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should successfully authenticate user and return tokens")
    void login_Success() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(loginRequest.getEmail())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access_jwt");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_jwt");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_jwt");
        verify(authenticationManager, times(1)).authenticate(any());
    }
}
