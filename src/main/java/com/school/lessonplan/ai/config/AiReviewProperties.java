// src/main/java/com/school/lessonplan/ai/config/AiReviewProperties.java
package com.school.lessonplan.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.review")
public class AiReviewProperties {

    private int minChars = 100;
    private int maxChars = 30000;
    private String sourceLabel = "Bộ thẩm định Chuẩn Sư phạm";
    private String legalBasisPath = "classpath:legal-basis.md";
    private int timeoutSeconds = 60;
    private int cacheExpireMinutes = 30;
    private int rateLimitPerMinute = 10;
    private GeminiConfig gemini = new GeminiConfig();

    public int getMinChars() {
        return minChars;
    }

    public void setMinChars(int minChars) {
        this.minChars = minChars;
    }

    public int getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getLegalBasisPath() {
        return legalBasisPath;
    }

    public void setLegalBasisPath(String legalBasisPath) {
        this.legalBasisPath = legalBasisPath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getCacheExpireMinutes() {
        return cacheExpireMinutes;
    }

    public void setCacheExpireMinutes(int cacheExpireMinutes) {
        this.cacheExpireMinutes = cacheExpireMinutes;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public GeminiConfig getGemini() {
        return gemini;
    }

    public void setGemini(GeminiConfig gemini) {
        this.gemini = gemini;
    }

    public static class GeminiConfig {
        private String apiKey;
        private String model = "gemini-3.8-flash";
        private String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
    }
}
