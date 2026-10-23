package com.school.lessonplan.feedback.dto;

import com.school.lessonplan.feedback.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackResponseForm {

    @NotNull(message = "Vui lòng chọn trạng thái xử lý.")
    private FeedbackStatus status;

    @Size(max = 4000, message = "Ý kiến phản hồi không quá 4000 ký tự.")
    private String adminResponse;

    public FeedbackResponseForm() {}

    public FeedbackResponseForm(FeedbackStatus status, String adminResponse) {
        this.status = status;
        this.adminResponse = adminResponse;
    }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse == null ? null : adminResponse.trim(); }
}
