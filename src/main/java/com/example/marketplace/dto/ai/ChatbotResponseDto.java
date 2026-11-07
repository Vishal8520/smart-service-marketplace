package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatbotResponseDto {
    private String reply;

    @JsonProperty("action_required")
    private String actionRequired;

    @JsonProperty("draft_booking")
    private Map<String, Object> draftBooking;

    @JsonProperty("tools_called")
    private List<Map<String, Object>> toolsCalled = new ArrayList<>();

    public ChatbotResponseDto() {}

    public ChatbotResponseDto(String reply) {
        this.reply = reply;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getActionRequired() { return actionRequired; }
    public void setActionRequired(String actionRequired) { this.actionRequired = actionRequired; }

    public Map<String, Object> getDraftBooking() { return draftBooking; }
    public void setDraftBooking(Map<String, Object> draftBooking) { this.draftBooking = draftBooking; }

    public List<Map<String, Object>> getToolsCalled() { return toolsCalled; }
    public void setToolsCalled(List<Map<String, Object>> toolsCalled) { this.toolsCalled = toolsCalled; }
}
