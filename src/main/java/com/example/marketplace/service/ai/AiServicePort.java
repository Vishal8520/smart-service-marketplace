package com.example.marketplace.service.ai;

import com.example.marketplace.dto.ai.*;
import java.util.List;

public interface AiServicePort {

    SmartSearchResponseDto smartSearch(String query, String city, int limit);

    RecommendationResponseDto getRecommendations(String query, String userCity, List<CandidateServiceDto> candidates, int limit);

    FraudScoreResponseDto scorePaymentFraud(PaymentFeaturesDto features);

    ReviewAnalysisDto analyzeReview(Long reviewId, Long serviceId, int rating, String comment);

    ListingAssistResponseDto assistListing(ListingAssistRequestDto request);

    ChatbotResponseDto sendChatMessage(ChatbotRequestDto request);

    void submitFeedback(AdminFeedbackRequestDto request);
}
