package com.example.marketplace.service.ai;

import com.example.marketplace.dto.ai.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceClient implements AiServicePort {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    private final String internalApiKey;

    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai.service.api-key:servenow-internal-ai-key-secret-2026}") String internalApiKey,
            @Value("${ai.service.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${ai.service.read-timeout-ms:4000}") int readTimeout) {

        this.aiServiceUrl = aiServiceUrl.replaceAll("/+$", "");
        this.internalApiKey = internalApiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-API-Key", internalApiKey);
        return headers;
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "smartSearchFallback")
    public SmartSearchResponseDto smartSearch(String query, String city, int limit) {
        String url = aiServiceUrl + "/api/v1/search/smart?q={q}&limit={limit}";
        Map<String, Object> uriVars = new HashMap<>();
        uriVars.put("q", query);
        uriVars.put("limit", limit);

        if (city != null && !city.isBlank()) {
            url += "&city={city}";
            uriVars.put("city", city);
        }

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<SmartSearchResponseDto> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SmartSearchResponseDto.class, uriVars);

        return response.getBody();
    }

    public SmartSearchResponseDto smartSearchFallback(String query, String city, int limit, Throwable t) {
        log.warn("AI Service smartSearch circuit open or timed out ({}). Using fallback empty response.", t.getMessage());
        return new SmartSearchResponseDto(query, null, new ArrayList<>(), 0);
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "recommendationsFallback")
    public RecommendationResponseDto getRecommendations(String query, String userCity, List<CandidateServiceDto> candidates, int limit) {
        String url = aiServiceUrl + "/api/v1/recommendations/rank";
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("query", query);
        reqBody.put("user_city", userCity);
        reqBody.put("candidates", candidates);
        reqBody.put("limit", limit);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, createHeaders());
        ResponseEntity<RecommendationResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, RecommendationResponseDto.class);

        return response.getBody();
    }

    public RecommendationResponseDto recommendationsFallback(String query, String userCity, List<CandidateServiceDto> candidates, int limit, Throwable t) {
        log.warn("AI Service getRecommendations fallback triggered: {}", t.getMessage());
        List<RankedServiceItemDto> items = new ArrayList<>();
        if (candidates != null) {
            for (CandidateServiceDto c : candidates) {
                items.add(new RankedServiceItemDto(
                        c.getId(), c.getTitle(), c.getCategory(), c.getPrice(), c.getRating(), c.getCity(),
                        0.5, "Standard marketplace recommendation"));
                if (items.size() >= limit) break;
            }
        }
        return new RecommendationResponseDto(items);
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "scorePaymentFraudFallback")
    public FraudScoreResponseDto scorePaymentFraud(PaymentFeaturesDto features) {
        String url = aiServiceUrl + "/api/v1/fraud/score";
        HttpEntity<PaymentFeaturesDto> entity = new HttpEntity<>(features, createHeaders());
        ResponseEntity<FraudScoreResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, FraudScoreResponseDto.class);

        return response.getBody();
    }

    public FraudScoreResponseDto scorePaymentFraudFallback(PaymentFeaturesDto features, Throwable t) {
        log.warn("AI Service fraud scoring fallback triggered: {}. Defaulting to manual check if reference reused.", t.getMessage());
        boolean isReused = Boolean.TRUE.equals(features.getIsReferenceReused());
        double score = isReused ? 0.90 : 0.05;
        String level = isReused ? "HIGH" : "LOW";
        List<String> reasons = List.of(isReused ? "Reused UPI reference detected" : "Fallback scoring: normal");
        return new FraudScoreResponseDto(features.getPaymentId(), score, level, reasons, isReused);
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "analyzeReviewFallback")
    public ReviewAnalysisDto analyzeReview(Long reviewId, Long serviceId, int rating, String comment) {
        String url = aiServiceUrl + "/api/v1/reviews/analyze";
        Map<String, Object> reqBody = Map.of(
                "review_id", reviewId,
                "service_id", serviceId,
                "rating", rating,
                "comment", comment != null ? comment : ""
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, createHeaders());
        ResponseEntity<ReviewAnalysisDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ReviewAnalysisDto.class);

        return response.getBody();
    }

    public ReviewAnalysisDto analyzeReviewFallback(Long reviewId, Long serviceId, int rating, String comment, Throwable t) {
        log.warn("AI Service review analysis fallback triggered: {}", t.getMessage());
        ReviewAnalysisDto dto = new ReviewAnalysisDto();
        dto.setReviewId(reviewId);
        dto.setSentiment(rating >= 4 ? "POSITIVE" : (rating <= 2 ? "NEGATIVE" : "NEUTRAL"));
        dto.setSentimentScore(0.0);
        dto.setIsSuspiciousOrFake(false);
        dto.setConfidence(0.5);
        return dto;
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "assistListingFallback")
    public ListingAssistResponseDto assistListing(ListingAssistRequestDto request) {
        String url = aiServiceUrl + "/api/v1/listing/assist";
        HttpEntity<ListingAssistRequestDto> entity = new HttpEntity<>(request, createHeaders());
        ResponseEntity<ListingAssistResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ListingAssistResponseDto.class);

        return response.getBody();
    }

    public ListingAssistResponseDto assistListingFallback(ListingAssistRequestDto request, Throwable t) {
        log.warn("AI Service listing assist fallback triggered: {}", t.getMessage());
        ListingAssistResponseDto dto = new ListingAssistResponseDto();
        dto.setSuggestedTitle("Professional " + request.getTitleKeywords() + " Service");
        dto.setGeneratedDescription("High-quality " + request.getCategory() + " service provided by verified professionals on ServeNow.");
        dto.setSuggestedPriceMin(299.0);
        dto.setSuggestedPriceMax(999.0);
        dto.setSuggestedPriceMedian(499.0);
        dto.setIsCompliant(true);
        return dto;
    }

    @Override
    @CircuitBreaker(name = "aiService", fallbackMethod = "sendChatMessageFallback")
    public ChatbotResponseDto sendChatMessage(ChatbotRequestDto request) {
        String url = aiServiceUrl + "/api/v1/chatbot/message";
        HttpEntity<ChatbotRequestDto> entity = new HttpEntity<>(request, createHeaders());
        ResponseEntity<ChatbotResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ChatbotResponseDto.class);

        return response.getBody();
    }

    public ChatbotResponseDto sendChatMessageFallback(ChatbotRequestDto request, Throwable t) {
        log.warn("AI Service chatbot fallback triggered: {}", t.getMessage());
        return new ChatbotResponseDto("Hello! Our AI assistant is currently performing maintenance. " +
                "You can still search services and manage your bookings directly via the marketplace tabs.");
    }

    @Override
    public void submitFeedback(AdminFeedbackRequestDto request) {
        try {
            String url = aiServiceUrl + "/api/v1/feedback";
            HttpEntity<AdminFeedbackRequestDto> entity = new HttpEntity<>(request, createHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (Exception e) {
            log.warn("Failed to record AI feedback: {}", e.getMessage());
        }
    }
}
