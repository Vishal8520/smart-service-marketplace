package com.example.marketplace.controller;

import com.example.marketplace.dto.ai.*;
import com.example.marketplace.entity.User;
import com.example.marketplace.repository.UserRepository;
import com.example.marketplace.service.ai.AiServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI Chatbot", description = "AI conversational booking assistant with tool calling and security bounds")
public class ChatbotController {

    private final AiServicePort aiServicePort;
    private final UserRepository userRepository;

    public ChatbotController(AiServicePort aiServicePort, UserRepository userRepository) {
        this.aiServicePort = aiServicePort;
        this.userRepository = userRepository;
    }

    @PostMapping("/message")
    @Operation(summary = "Send message to AI booking assistant (Acts strictly with authenticated user permissions)")
    public ResponseEntity<ChatbotResponseDto> sendMessage(
            @RequestBody ChatbotRequestDto request,
            Authentication authentication) {

        String userEmail = authentication != null ? authentication.getName() : "anonymous@servenow.com";
        User user = userRepository.findByEmail(userEmail).orElse(null);

        Long userId = user != null ? user.getId() : 0L;
        String userName = user != null ? user.getName() : "Guest";
        String userRole = user != null && user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER";

        ChatbotContextDto context = new ChatbotContextDto(userId, userName, userRole, userEmail);
        request.setContext(context);

        if (request.getConversationHistory() == null) {
            request.setConversationHistory(new ArrayList<>());
        }

        ChatbotResponseDto response = aiServicePort.sendChatMessage(request);
        return ResponseEntity.ok(response);
    }
}
