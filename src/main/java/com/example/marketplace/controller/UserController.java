package com.example.marketplace.controller;

import com.example.marketplace.entity.RoleType;
import com.example.marketplace.entity.User;
import com.example.marketplace.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Passwordless user management for customers and providers")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/quick-register")
    @Operation(summary = "Passwordless registration for Customer or Service Provider")
    public ResponseEntity<User> quickRegister(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.get("name");
        String phone = payload.get("phone");
        String roleStr = payload.getOrDefault("role", "CUSTOMER");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            RoleType role = RoleType.CUSTOMER;
            try {
                role = RoleType.valueOf(roleStr);
            } catch (Exception ignored) {
            }
            User newUser = User.builder()
                    .name(name != null ? name : "User")
                    .email(email)
                    .phone(phone)
                    .role(role)
                    .build();
            return userRepository.save(newUser);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/by-email")
    @Operation(summary = "Lookup user by email")
    public ResponseEntity<User> getByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
