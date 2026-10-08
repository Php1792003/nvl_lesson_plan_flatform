package com.school.lessonplan.role;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.role.dto.RoleCreateForm;
import com.school.lessonplan.role.dto.RoleUpdateForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public String listRoles(Model model) {
        loadRoleLookups(model);
        if (!model.containsAttribute("createRoleForm")) {
            model.addAttribute("createRoleForm", new RoleCreateForm());
        }
        if (!model.containsAttribute("updateRoleForm")) {
            model.addAttribute("updateRoleForm", new RoleUpdateForm());
        }
        return "admin/roles/role-permission-manager";
    }

    private void loadRoleLookups(Model model) {
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("allPermissions", roleService.getAllPermissions());
    }

    @PostMapping
    public String createRole(
            @Valid @ModelAttribute("createRoleForm") RoleCreateForm createRoleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                roleService.createRole(createRoleForm.getCode(), createRoleForm.getName(), createRoleForm.getPermissionIds());
                redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
                return "redirect:/admin/roles";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadRoleLookups(model);
        if (!model.containsAttribute("updateRoleForm")) {
            model.addAttribute("updateRoleForm", new RoleUpdateForm());
        }
        model.addAttribute("showCreateModal", true);
        return "admin/roles/role-permission-manager";
    }

    @PostMapping("/{id}")
    public String updateRole(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("updateRoleForm") RoleUpdateForm updateRoleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                roleService.updateRole(id, updateRoleForm.getName(), updateRoleForm.getPermissionIds());
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
                return "redirect:/admin/roles";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadRoleLookups(model);
        if (!model.containsAttribute("createRoleForm")) {
            model.addAttribute("createRoleForm", new RoleCreateForm());
        }
        model.addAttribute("activeEditRoleId", id);
        return "admin/roles/role-permission-manager";
    }
}
