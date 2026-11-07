package com.example.marketplace.service.ai;

import com.example.marketplace.dto.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AiServiceClientTest {

    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        // Point to an unreachable mock port to verify circuit breaker & fallback robustness
        aiServiceClient = new AiServiceClient(
                "http://localhost:59999",
                "test-secret-key",
                500,
                500
        );
    }

    @Test
    @DisplayName("smartSearchFallback returns clean empty response on service timeout")
    void smartSearchFallback_returnsEmptyResponse() {
        Throwable cause = new RuntimeException("Connection refused");
        SmartSearchResponseDto fallback = aiServiceClient.smartSearchFallback("leaking pipe", "Bengaluru", 5, cause);

        assertNotNull(fallback);
        assertEquals("leaking pipe", fallback.getQuery());
        assertEquals(0, fallback.getTotal());
        assertTrue(fallback.getResults().isEmpty());
    }

    @Test
    @DisplayName("recommendationsFallback gracefully degrades to baseline candidates")
    void recommendationsFallback_returnsCandidateList() {
        Throwable cause = new RuntimeException("Read timeout");
        CandidateServiceDto c1 = new CandidateServiceDto(1L, "Plumbing", "Plumbing", 300.0, 4.5, "Bengaluru", 0.9, 10L);

        RecommendationResponseDto fallback = aiServiceClient.recommendationsFallback(
                "plumber", "Bengaluru", List.of(c1), 5, cause);

        assertNotNull(fallback);
        assertEquals(1, fallback.getRecommendations().size());
        assertEquals("Plumbing", fallback.getRecommendations().get(0).getTitle());
    }

    @Test
    @DisplayName("scorePaymentFraudFallback evaluates reused reference as HIGH risk")
    void scorePaymentFraudFallback_evaluatesReusedRef() {
        Throwable cause = new RuntimeException("Network down");
        PaymentFeaturesDto features = new PaymentFeaturesDto();
        features.setPaymentId(101L);
        features.setIsReferenceReused(true);

        FraudScoreResponseDto fallback = aiServiceClient.scorePaymentFraudFallback(features, cause);

        assertNotNull(fallback);
        assertEquals(101L, fallback.getPaymentId());
        assertEquals("HIGH", fallback.getRiskLevel());
        assertTrue(fallback.getFlaggedForReview());
    }

    @Test
    @DisplayName("scorePaymentFraudFallback evaluates normal reference as LOW risk")
    void scorePaymentFraudFallback_evaluatesNormalRef() {
        Throwable cause = new RuntimeException("Network down");
        PaymentFeaturesDto features = new PaymentFeaturesDto();
        features.setPaymentId(102L);
        features.setIsReferenceReused(false);

        FraudScoreResponseDto fallback = aiServiceClient.scorePaymentFraudFallback(features, cause);

        assertNotNull(fallback);
        assertEquals(102L, fallback.getPaymentId());
        assertEquals("LOW", fallback.getRiskLevel());
        assertFalse(fallback.getFlaggedForReview());
    }

    @Test
    @DisplayName("sendChatMessageFallback returns helpful maintenance notification")
    void sendChatMessageFallback_returnsHelpfulMessage() {
        Throwable cause = new RuntimeException("AI service unavailable");
        ChatbotRequestDto request = new ChatbotRequestDto("Hello", List.of(), new ChatbotContextDto(1L, "John", "ROLE_CUSTOMER", "john@example.com"));

        ChatbotResponseDto fallback = aiServiceClient.sendChatMessageFallback(request, cause);

        assertNotNull(fallback);
        assertTrue(fallback.getReply().contains("currently performing maintenance") || fallback.getReply().contains("marketplace tabs"));
    }

    @Test
    @DisplayName("assistListingFallback provides standard listing defaults")
    void assistListingFallback_returnsDefaults() {
        Throwable cause = new RuntimeException("Timeout");
        ListingAssistRequestDto req = new ListingAssistRequestDto("Plumbing", "tap repair", "fix leaky tap", "Bengaluru", List.of(300.0, 500.0));

        ListingAssistResponseDto fallback = aiServiceClient.assistListingFallback(req, cause);

        assertNotNull(fallback);
        assertTrue(fallback.getSuggestedTitle().contains("tap repair"));
        assertTrue(fallback.getIsCompliant());
        assertEquals(499.0, fallback.getSuggestedPriceMedian());
    }
}
