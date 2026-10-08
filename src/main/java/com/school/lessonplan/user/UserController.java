package com.school.lessonplan.user;

import com.school.lessonplan.auth.dto.ChangePasswordForm;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.dto.ProfileEditForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class UserController {

    private final UserService userService;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;

    public UserController(UserService userService,
                          DepartmentRepository departmentRepository,
                          CampusRepository campusRepository) {
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
    }

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        User user = userService.getById(currentUser.getId());
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        if (!model.containsAttribute("profileEditForm")) {
            ProfileEditForm editForm = new ProfileEditForm();
            if (user != null) {
                editForm.setLastName(user.getLastName());
                editForm.setGivenName(user.getGivenName());
                if (user.getDepartment() != null) editForm.setDepartmentId(user.getDepartment().getId());
                if (user.getCampus() != null) editForm.setCampusId(user.getCampus().getId());
            }
            model.addAttribute("profileEditForm", editForm);
        }
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        return "user/user-profile-view";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @ModelAttribute("profileEditForm") ProfileEditForm profileEditForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                userService.updateProfile(currentUser.getId(),
                        profileEditForm.getLastName(),
                        profileEditForm.getGivenName(),
                        profileEditForm.getDepartmentId(),
                        profileEditForm.getCampusId());
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
                return "redirect:/profile";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        User user = userService.getById(currentUser.getId());
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        return "user/user-profile-view";
    }
}
