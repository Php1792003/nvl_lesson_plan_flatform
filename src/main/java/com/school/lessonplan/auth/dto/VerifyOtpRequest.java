package com.school.lessonplan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyOtpRequest {

    @NotBlank(message = "{otp.code.required}")
    @Size(min = 6, max = 6, message = "{otp.code.size}")
    private String code;

    private String email; // dùng cho quên mật khẩu

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
