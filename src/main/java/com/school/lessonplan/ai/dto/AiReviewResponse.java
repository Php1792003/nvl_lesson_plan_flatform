// src/main/java/com/school/lessonplan/ai/dto/AiReviewResponse.java
package com.school.lessonplan.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class AiReviewResponse {

    private boolean success = true;
    private InputType inputType;
    private ReviewVerdict verdict;
    private String badgeText;
    private String summary;
    private List<String> passedPoints = new ArrayList<>();
    private List<ChecklistItem> checklist = new ArrayList<>();
    private String conclusionText;
    private String bghFollowUp = "";
    private String sourceLabel = "Bộ thẩm định Chuẩn Sư phạm";
    private String errorMessage;

    public AiReviewResponse() {}

    public static AiReviewResponse error(String message) {
        AiReviewResponse res = new AiReviewResponse();
        res.setSuccess(false);
        res.setVerdict(ReviewVerdict.KHONG_DUYET);
        res.setInputType(InputType.GIAO_AN_KHONG_DAY_DU);
        res.setBadgeText("Lỗi thẩm định");
        res.setSummary(message);
        res.setErrorMessage(message);
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public ReviewVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(ReviewVerdict verdict) {
        this.verdict = verdict;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public void setBadgeText(String badgeText) {
        this.badgeText = badgeText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getPassedPoints() {
        return passedPoints;
    }

    public void setPassedPoints(List<String> passedPoints) {
        this.passedPoints = passedPoints != null ? passedPoints : new ArrayList<>();
    }

    public List<ChecklistItem> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = checklist != null ? checklist : new ArrayList<>();
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public String getBghFollowUp() {
        return bghFollowUp;
    }

    public void setBghFollowUp(String bghFollowUp) {
        this.bghFollowUp = bghFollowUp != null ? bghFollowUp : "";
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
