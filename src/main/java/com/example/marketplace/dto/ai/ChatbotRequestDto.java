package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ChatbotRequestDto {
    private String message;

    @JsonProperty("conversation_history")
    private List<ChatMessageDto> conversationHistory = new ArrayList<>();

    private ChatbotContextDto context;

    public ChatbotRequestDto() {}

    public ChatbotRequestDto(String message, List<ChatMessageDto> conversationHistory, ChatbotContextDto context) {
        this.message = message;
        this.conversationHistory = conversationHistory != null ? conversationHistory : new ArrayList<>();
        this.context = context;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessageDto> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ChatMessageDto> conversationHistory) { this.conversationHistory = conversationHistory; }

    public ChatbotContextDto getContext() { return context; }
    public void setContext(ChatbotContextDto context) { this.context = context; }
}
