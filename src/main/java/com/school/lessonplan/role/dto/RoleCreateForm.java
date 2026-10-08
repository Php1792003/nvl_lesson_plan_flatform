package com.school.lessonplan.role.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@GroupSequence({RoleCreateForm.class, FormatGroup.class})
public class RoleCreateForm {

    @NotBlank
    @Size(min = 3, max = 30, groups = FormatGroup.class)
    @Pattern(regexp = "^[A-Z0-9_]{3,30}$", groups = FormatGroup.class)
    private String code;

    @NotBlank
    @Size(max = 100, groups = FormatGroup.class)
    private String name;

    private List<Long> permissionIds = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds != null ? permissionIds : new ArrayList<>();
    }
}
