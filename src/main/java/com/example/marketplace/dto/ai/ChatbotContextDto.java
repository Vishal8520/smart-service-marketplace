package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatbotContextDto {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_role")
    private String userRole;

    @JsonProperty("user_email")
    private String userEmail;

    public ChatbotContextDto() {}

    public ChatbotContextDto(Long userId, String userName, String userRole, String userEmail) {
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.userEmail = userEmail;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
