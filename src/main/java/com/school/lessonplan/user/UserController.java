package com.school.lessonplan.user;

import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return "user/profile";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("lastName") String lastName,
            @RequestParam("givenName") String givenName,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("campusId") Long campusId,
            RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            userService.updateProfile(currentUser.getId(), lastName, givenName, departmentId, campusId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }
}
