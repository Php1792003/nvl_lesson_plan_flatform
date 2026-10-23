package com.school.lessonplan.feedback.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import com.school.lessonplan.feedback.FeedbackCategory;
import com.school.lessonplan.feedback.FeedbackPriority;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@GroupSequence({FeedbackCreateForm.class, FormatGroup.class})
public class FeedbackCreateForm {

    @NotNull(message = "Vui lòng chọn phân loại góp ý.")
    private FeedbackCategory category = FeedbackCategory.CHUYEN_MON;

    @NotNull(message = "Vui lòng chọn mức độ ưu tiên.")
    private FeedbackPriority priority = FeedbackPriority.NORMAL;

    @NotBlank(message = "Vui lòng nhập tiêu đề góp ý.")
    @Size(max = 250, message = "Tiêu đề không quá 250 ký tự.", groups = FormatGroup.class)
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung chi tiết.")
    @Size(min = 10, max = 5000, message = "Nội dung cần từ 10 đến 5000 ký tự.", groups = FormatGroup.class)
    private String content;

    @NotBlank(message = "Vui lòng nhập họ và tên của Thầy/Cô.")
    @Size(max = 200, groups = FormatGroup.class)
    private String senderName;

    @NotBlank(message = "Vui lòng nhập email liên hệ.")
    @Email(message = "Email không đúng định dạng.", groups = FormatGroup.class)
    @Size(max = 320, groups = FormatGroup.class)
    private String senderEmail;

    @Size(max = 200, groups = FormatGroup.class)
    private String departmentName;

    public FeedbackCreateForm() {}

    public FeedbackCategory getCategory() { return category; }
    public void setCategory(FeedbackCategory category) { this.category = category; }

    public FeedbackPriority getPriority() { return priority; }
    public void setPriority(FeedbackPriority priority) { this.priority = priority; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? null : title.trim(); }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? null : content.trim(); }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName == null ? null : senderName.trim(); }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail == null ? null : senderEmail.trim().toLowerCase(); }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName == null ? null : departmentName.trim(); }
}
