package com.school.lessonplan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "{register.lastName.required}")
    @Size(max = 100, message = "{register.lastName.maxLength}")
    private String lastName;

    @NotBlank(message = "{register.givenName.required}")
    @Size(max = 200, message = "{register.givenName.maxLength}")
    private String givenName;

    @NotBlank(message = "{register.email.required}")
    @Email(message = "{register.email.invalid}")
    @Size(max = 320, message = "{register.email.maxLength}")
    private String email;

    @NotNull(message = "{register.department.required}")
    private Long departmentId;

    private Long campusId;

    @NotBlank(message = "{register.password.required}")
    @Size(min = 8, max = 128, message = "{register.password.size}")
    private String password;

    @NotBlank(message = "{register.confirmPassword.required}")
    private String confirmPassword;

    // Getters and Setters
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
