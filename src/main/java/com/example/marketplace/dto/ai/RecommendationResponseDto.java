package com.example.marketplace.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class RecommendationResponseDto {
    private List<RankedServiceItemDto> recommendations = new ArrayList<>();

    public RecommendationResponseDto() {}

    public RecommendationResponseDto(List<RankedServiceItemDto> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public List<RankedServiceItemDto> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RankedServiceItemDto> recommendations) { this.recommendations = recommendations; }
}
