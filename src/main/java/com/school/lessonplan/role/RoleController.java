package com.school.lessonplan.role;

import com.school.lessonplan.common.exception.BusinessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("allPermissions", roleService.getAllPermissions());
        return "admin/roles/list";
    }

    @PostMapping
    public String createRole(@RequestParam("code") String code,
                             @RequestParam("name") String name,
                             @RequestParam(name = "permissionIds", required = false) List<Long> permissionIds,
                             RedirectAttributes redirectAttributes) {
        try {
            roleService.createRole(code, name, permissionIds);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}")
    public String updateRole(@PathVariable("id") Long id,
                             @RequestParam("name") String name,
                             @RequestParam(name = "permissionIds", required = false) List<Long> permissionIds,
                             RedirectAttributes redirectAttributes) {
        try {
            roleService.updateRole(id, name, permissionIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
