package com.school.lessonplan.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class AiReviewResponse {

    private boolean success;
    private String source; // "GEMINI_AI" or "PEDAGOGICAL_ENGINE"
    private String modelName;
    private String overallRating; // "Xuất sắc", "Tốt", "Khá", "Đạt yêu cầu"
    private String summary;
    private List<String> strengths = new ArrayList<>();
    private List<String> improvements = new ArrayList<>();
    private String suggestedFeedback;
    private String errorMessage;

    public AiReviewResponse() {}

    public static AiReviewResponse error(String message) {
        AiReviewResponse res = new AiReviewResponse();
        res.setSuccess(false);
        res.setErrorMessage(message);
        return res;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getOverallRating() { return overallRating; }
    public void setOverallRating(String overallRating) { this.overallRating = overallRating; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }

    public String getSuggestedFeedback() { return suggestedFeedback; }
    public void setSuggestedFeedback(String suggestedFeedback) { this.suggestedFeedback = suggestedFeedback; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
