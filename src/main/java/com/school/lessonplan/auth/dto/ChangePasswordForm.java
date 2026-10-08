package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import com.school.lessonplan.common.validation.PasswordConfirmable;
import com.school.lessonplan.common.validation.PasswordMatches;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({ChangePasswordForm.class, FormatGroup.class})
@PasswordMatches(groups = FormatGroup.class)
public class ChangePasswordForm implements PasswordConfirmable {

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 72, groups = FormatGroup.class)
    @Pattern(regexp = PASSWORD_REGEX, groups = FormatGroup.class)
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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
