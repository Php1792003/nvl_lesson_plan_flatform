package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({ForgotPasswordForm.class, FormatGroup.class})
public class ForgotPasswordForm {

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    @NotBlank
    @Size(max = 150, groups = FormatGroup.class)
    @Pattern(regexp = EMAIL_REGEX, groups = FormatGroup.class)
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
