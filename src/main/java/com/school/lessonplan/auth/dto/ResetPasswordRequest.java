package com.school.lessonplan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String token; // session token sau khi verify OTP thành công

    @NotBlank(message = "{password.new.required}")
    @Size(min = 8, max = 128, message = "{password.size}")
    private String newPassword;

    @NotBlank(message = "{password.confirm.required}")
    private String confirmPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
