package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import com.school.lessonplan.common.validation.PasswordConfirmable;
import com.school.lessonplan.common.validation.PasswordMatches;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({RegisterRequest.class, FormatGroup.class})
@PasswordMatches(groups = FormatGroup.class)
public class RegisterRequest implements PasswordConfirmable {

    public static final String NAME_REGEX = "^[\\p{L}][\\p{L} .'’-]*$";
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    @NotBlank
    @Size(max = 50, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String lastName;

    @NotBlank
    @Size(max = 80, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String givenName;

    @NotBlank
    @Size(max = 150, groups = FormatGroup.class)
    @Pattern(regexp = EMAIL_REGEX, groups = FormatGroup.class)
    private String email;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long campusId;

    @NotBlank
    @Size(min = 8, max = 72, groups = FormatGroup.class)
    @Pattern(regexp = PASSWORD_REGEX, groups = FormatGroup.class)
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotNull(message = "Vui lòng tích chọn đồng ý với Điều khoản sử dụng và Chính sách bảo mật.")
    @AssertTrue(message = "Vui lòng tích chọn đồng ý với Điều khoản sử dụng và Chính sách bảo mật.", groups = {RegisterRequest.class, FormatGroup.class})
    private Boolean agreeTerms;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName == null ? null : lastName.trim();
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName == null ? null : givenName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long campusId) {
        this.campusId = campusId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Boolean getAgreeTerms() {
        return agreeTerms;
    }

    public void setAgreeTerms(Boolean agreeTerms) {
        this.agreeTerms = agreeTerms;
    }
}
