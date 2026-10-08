package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({VerifyPasswordResetOtpForm.class, FormatGroup.class})
public class VerifyPasswordResetOtpForm {

    public static final String OTP_REGEX = "^[0-9]{6}$";

    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 6, groups = FormatGroup.class)
    @Pattern(regexp = OTP_REGEX, groups = FormatGroup.class)
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
