package com.school.lessonplan.user.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@GroupSequence({AdminUserUpdateForm.class, FormatGroup.class})
public class AdminUserUpdateForm {

    public static final String NAME_REGEX = "^[\\p{L}][\\p{L} .'’-]*$";

    @NotBlank
    @Size(max = 50, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String lastName;

    @NotBlank
    @Size(max = 80, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String givenName;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long campusId;

    private List<Long> roleIds = new ArrayList<>();

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

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds != null ? roleIds : new ArrayList<>();
    }
}
