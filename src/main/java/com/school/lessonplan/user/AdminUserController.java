package com.school.lessonplan.user;

import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.dto.AdminUserCreateForm;
import com.school.lessonplan.user.dto.AdminUserUpdateForm;
import com.school.lessonplan.user.dto.DeleteUserForm;
import com.school.lessonplan.user.dto.LockUserForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.school.lessonplan.lessonplan.LessonPlanRepository;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_VIEW')")
public class AdminUserController {

    private final UserService userService;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final RoleRepository roleRepository;
    private final LessonPlanRepository lessonPlanRepository;

    public AdminUserController(UserService userService,
                               DepartmentRepository departmentRepository,
                               CampusRepository campusRepository,
                               RoleRepository roleRepository,
                               LessonPlanRepository lessonPlanRepository) {
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.roleRepository = roleRepository;
        this.lessonPlanRepository = lessonPlanRepository;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) UserStatus status,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "campusId", required = false) Long campusId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Page<User> usersPage = userService.searchUsers(
            search, status, departmentId, campusId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("campusId", campusId);
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());

        return "admin/users/user-directory";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")
    public String newUserForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new AdminUserCreateForm());
        }
        loadUserLookups(model);
        return "admin/users/admin-user-creator";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")
    public String createUser(
            @Valid @ModelAttribute("userForm") AdminUserCreateForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.createUser(
                        userForm.getLastName(),
                        userForm.getGivenName(),
                        userForm.getEmail(),
                        userForm.getPassword(),
                        userForm.getDepartmentId(),
                        userForm.getCampusId(),
                        userForm.getRoleIds()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
                return "redirect:/admin/users";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadUserLookups(model);
        userForm.setPassword(null);
        return "admin/users/admin-user-creator";
    }

    private void loadUserLookups(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id, Model model) {
        loadUserDetailLookups(id, model);
        return "admin/users/admin-user-detail-view";
    }

    private void loadUserDetailLookups(Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("planCount", lessonPlanRepository.countByOwnerId(id));

        if (!model.containsAttribute("userForm")) {
            AdminUserUpdateForm form = new AdminUserUpdateForm();
            form.setLastName(user.getLastName());
            form.setGivenName(user.getGivenName());
            if (user.getDepartment() != null) form.setDepartmentId(user.getDepartment().getId());
            if (user.getCampus() != null) form.setCampusId(user.getCampus().getId());
            List<Long> roleIds = user.getRoles().stream().map(r -> r.getId()).toList();
            form.setRoleIds(roleIds);
            model.addAttribute("userForm", form);
        }
        if (!model.containsAttribute("lockUserForm")) {
            model.addAttribute("lockUserForm", new LockUserForm());
        }
        if (!model.containsAttribute("deleteUserForm")) {
            model.addAttribute("deleteUserForm", new DeleteUserForm());
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_UPDATE')")
    public String updateUser(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("userForm") AdminUserUpdateForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.updateUser(
                        id,
                        userForm.getLastName(),
                        userForm.getGivenName(),
                        userForm.getDepartmentId(),
                        userForm.getCampusId(),
                        userForm.getRoleIds()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
                return "redirect:/admin/users/" + id;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadUserDetailLookups(id, model);
        return "admin/users/admin-user-detail-view";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_UPDATE')")
    public String approveUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt tài khoản thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_LOCK')")
    public String lockUser(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("lockUserForm") LockUserForm lockUserForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.lockUser(id, lockUserForm.getReason());
                redirectAttributes.addFlashAttribute("successMessage", "Đã khoá tài khoản!");
                return "redirect:/admin/users/" + id;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadUserDetailLookups(id, model);
        model.addAttribute("showLockModal", true);
        return "admin/users/admin-user-detail-view";
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_LOCK')")
    public String unlockUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.unlockUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã mở khoá tài khoản!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_DELETE')")
    public String deleteUser(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("deleteUserForm") DeleteUserForm deleteUserForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal CurrentUser currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.deleteUser(id, deleteUserForm.getConfirmEmail(), currentUser);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xoá tài khoản giáo viên thành công! Các giáo án của giáo viên đã được chuyển thành giáo án mồ côi và lưu giữ an toàn.");
                return "redirect:/admin/users";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadUserDetailLookups(id, model);
        model.addAttribute("showDeleteModal", true);
        return "admin/users/admin-user-detail-view";
    }
}
