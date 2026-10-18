// src/main/java/com/school/lessonplan/ai/client/GeminiAiClient.java
package com.school.lessonplan.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiClient.class);

    private final AiReviewProperties aiReviewProperties;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAiClient(AiReviewProperties aiReviewProperties,
                          AppProperties appProperties,
                          ObjectMapper objectMapper) {
        this.aiReviewProperties = aiReviewProperties;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private String getApiKey() {
        if (aiReviewProperties.getGemini() != null &&
                aiReviewProperties.getGemini().getApiKey() != null &&
                !aiReviewProperties.getGemini().getApiKey().isBlank() &&
                !aiReviewProperties.getGemini().getApiKey().equalsIgnoreCase("none")) {
            return aiReviewProperties.getGemini().getApiKey().trim();
        }
        if (appProperties.getAi() != null &&
                appProperties.getAi().getGemini() != null &&
                appProperties.getAi().getGemini().getApiKey() != null &&
                !appProperties.getAi().getGemini().getApiKey().isBlank()) {
            return appProperties.getAi().getGemini().getApiKey().trim();
        }
        return "";
    }

    private String getModel() {
        if (aiReviewProperties.getGemini() != null &&
                aiReviewProperties.getGemini().getModel() != null &&
                !aiReviewProperties.getGemini().getModel().isBlank()) {
            return aiReviewProperties.getGemini().getModel().trim();
        }
        if (appProperties.getAi() != null &&
                appProperties.getAi().getGemini() != null &&
                appProperties.getAi().getGemini().getModel() != null &&
                !appProperties.getAi().getGemini().getModel().isBlank()) {
            return appProperties.getAi().getGemini().getModel().trim();
        }
        return "gemini-3.8-flash";
    }

    private String getApiUrl() {
        if (aiReviewProperties.getGemini() != null &&
                aiReviewProperties.getGemini().getApiUrl() != null &&
                !aiReviewProperties.getGemini().getApiUrl().isBlank()) {
            return aiReviewProperties.getGemini().getApiUrl().trim();
        }
        return "https://generativelanguage.googleapis.com/v1beta/models";
    }

    @Override
    public boolean isConfigured() {
        String key = getApiKey();
        return !key.isBlank() && !key.equalsIgnoreCase("none");
    }

    @Override
    public String complete(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Gemini API Key chưa được cấu hình.");
        }

        String apiKey = getApiKey();
        String model = getModel();
        String baseUrl = getApiUrl();
        String endpoint = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

        try {
            // Build Gemini request payload with systemInstruction and user content
            Map<String, Object> requestPayload = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 4096,
                            "responseMimeType", "application/json"
                    )
            );

            String requestBody = objectMapper.writeValueAsString(requestPayload);
            int timeoutSeconds = aiReviewProperties.getTimeoutSeconds() > 0
                    ? aiReviewProperties.getTimeoutSeconds() : 60;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            log.info("Calling Google Gemini API model={} timeout={}s", model, timeoutSeconds);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).path("text").asText();
                    }
                }
                throw new RuntimeException("Phản hồi từ Google Gemini không chứa nội dung hợp lệ.");
            } else if (response.statusCode() == 401) {
                log.error("Google Gemini API Key invalid or expired (Status 401): {}", response.body());
                throw new RuntimeException("Khóa Google Gemini API Key chưa hợp lệ hoặc đã hết hạn (HTTP 401). Khóa API hợp lệ từ Google AI Studio có tiền tố 'AIzaSy...'.");
            } else if (response.statusCode() == 429) {
                log.error("Google Gemini API quota exceeded (Status 429): {}", response.body());
                throw new RuntimeException("Google Gemini API đã vượt quá giới hạn truy vấn (HTTP 429 - Quota Exceeded). Vui lòng thử lại sau.");
            } else {
                log.error("Google Gemini API error (Status: {}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Google Gemini API trả về mã lỗi HTTP: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Gemini AI request execution failed: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối tới dịch vụ AI Gemini: " + e.getMessage(), e);
        }
    }
}
