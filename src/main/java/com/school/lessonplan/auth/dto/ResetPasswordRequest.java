package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import com.school.lessonplan.common.validation.PasswordConfirmable;
import com.school.lessonplan.common.validation.PasswordMatches;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({ResetPasswordRequest.class, FormatGroup.class})
@PasswordMatches(groups = FormatGroup.class)
public class ResetPasswordRequest implements PasswordConfirmable {

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    @NotBlank
    private String email;

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, max = 72, groups = FormatGroup.class)
    @Pattern(regexp = PASSWORD_REGEX, groups = FormatGroup.class)
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String getPassword() {
        return newPassword;
    }

    @Override
    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
