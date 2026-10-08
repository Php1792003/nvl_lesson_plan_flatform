package com.school.lessonplan.user.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@GroupSequence({LockUserForm.class, FormatGroup.class})
public class LockUserForm {

    @NotBlank
    @Size(min = 5, max = 500, groups = FormatGroup.class)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? null : reason.trim();
    }
}
