package com.school.lessonplan.user;

import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_VIEW')")
public class AdminUserController {

    private final UserService userService;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final RoleRepository roleRepository;

    public AdminUserController(UserService userService,
                               DepartmentRepository departmentRepository,
                               CampusRepository campusRepository,
                               RoleRepository roleRepository) {
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.roleRepository = roleRepository;
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

        return "admin/users/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")
    public String newUserForm(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/users/create";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")
    public String createUser(
            @RequestParam("lastName") String lastName,
            @RequestParam("givenName") String givenName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("campusId") Long campusId,
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds,
            RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(lastName, givenName, email, password, departmentId, campusId, roleIds);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/users/detail";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_UPDATE')")
    public String updateUser(
            @PathVariable("id") Long id,
            @RequestParam("lastName") String lastName,
            @RequestParam("givenName") String givenName,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("campusId") Long campusId,
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, lastName, givenName, departmentId, campusId, roleIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
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
            @RequestParam("reason") String reason,
            RedirectAttributes redirectAttributes) {
        try {
            userService.lockUser(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khoá tài khoản!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
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
            @RequestParam("confirmEmail") String confirmEmail,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id, confirmEmail, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã xoá tài khoản giáo viên thành công! Các giáo án của giáo viên đã được chuyển thành giáo án mồ côi và lưu giữ an toàn.");
            return "redirect:/admin/users";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }
}
