package com.school.lessonplan.auth;

import com.school.lessonplan.auth.dto.*;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.common.exception.RateLimitException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.security.RateLimiterService;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;
    private final RateLimiterService rateLimiterService;
    private final com.school.lessonplan.security.CookieService cookieService;

    public AuthController(AuthService authService,
                          DepartmentRepository departmentRepository,
                          CampusRepository campusRepository,
                          UserRepository userRepository,
                          RateLimiterService rateLimiterService,
                          com.school.lessonplan.security.CookieService cookieService) {
        this.authService = authService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
        this.cookieService = cookieService;
    }

    // ======================== LOGIN ========================

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal CurrentUser currentUser,
                            HttpServletRequest request,
                            Model model) {
        if (currentUser != null) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("loginRequest")) {
            LoginRequest loginRequest = new LoginRequest();
            String savedEmail = cookieService.getRememberEmail(request);
            if (savedEmail != null && !savedEmail.isBlank()) {
                loginRequest.setEmail(savedEmail);
                loginRequest.setRememberMe(true);
            }
            model.addAttribute("loginRequest", loginRequest);
        }
        return "auth/login-page";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loginRequest.setPassword(null);
            return "auth/login-page";
        }

        try {
            rateLimiterService.checkLoginRateLimit(request.getRemoteAddr() + ":" + loginRequest.getEmail());
            authService.login(loginRequest, request, response);
            return "redirect:/dashboard";
        } catch (BusinessException | RateLimitException e) {
            model.addAttribute("errorMessage", e.getMessage());
            loginRequest.setPassword(null);
            return "auth/login-page";
        }
    }

    // ======================== LOGOUT ========================

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return "redirect:/login?logout";
    }

    // ======================== REFRESH TOKEN ========================

    @PostMapping("/auth/refresh")
    @ResponseBody
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        boolean refreshed = authService.refreshAccessToken(request, response);
        return refreshed ? "{\"status\":\"ok\"}" : "{\"status\":\"unauthorized\"}";
    }

    // ======================== REGISTER ========================

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser != null) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        loadRegisterLookups(model);
        return "auth/user-registration-form";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                rateLimiterService.checkRegisterRateLimit(request.getRemoteAddr());
                User user = authService.register(registerRequest);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đăng ký thành công! Vui lòng nhập mã xác thực OTP đã được gửi đến email của bạn.");
                return "redirect:/verify-email?userId=" + user.getId();
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        loadRegisterLookups(model);
        registerRequest.setPassword(null);
        registerRequest.setConfirmPassword(null);
        return "auth/user-registration-form";
    }

    private void loadRegisterLookups(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
    }

    // ======================== EMAIL OTP VERIFICATION ========================

    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam(name = "userId", required = false) Long userId,
                                  Model model) {
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("userId", userId);
        if (!model.containsAttribute("verifyOtpRequest")) {
            model.addAttribute("verifyOtpRequest", new VerifyOtpRequest());
        }
        return "auth/email-verification-page";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam("userId") Long userId,
                              @Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest verifyOtpRequest,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + user.getEmail());
                authService.verifyEmailOtp(userId, verifyOtpRequest.getCode());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Xác thực email thành công! Tài khoản đang chờ Ban Giám Hiệu phê duyệt. Bạn sẽ nhận được thông báo qua email khi tài khoản được kích hoạt.");
                return "redirect:/login";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("userId", userId);
        return "auth/email-verification-page";
    }

    @PostMapping("/verify-email/resend")
    public String resendEmailOtp(@RequestParam("userId") Long userId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + user.getEmail());
            authService.sendVerificationOtp(user);
            redirectAttributes.addFlashAttribute("successMessage", "Mã xác thực mới đã được gửi vào email của bạn.");
        } catch (BusinessException | RateLimitException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/verify-email?userId=" + userId;
    }

    // ======================== FORGOT PASSWORD ========================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/password-recovery-request";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm forgotPasswordForm,
                                 BindingResult bindingResult,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/password-recovery-request";
        }

        try {
            rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + forgotPasswordForm.getEmail());
            authService.sendPasswordResetOtp(forgotPasswordForm.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Nếu email tồn tại trong hệ thống, mã xác thực đã được gửi đến email của bạn.");
            return "redirect:/forgot-password/verify?email=" + forgotPasswordForm.getEmail().trim().toLowerCase();
        } catch (RateLimitException e) {
            bindingResult.reject("form.error", e.getMessage());
            return "auth/password-recovery-request";
        }
    }

    @GetMapping("/forgot-password/verify")
    public String verifyPasswordResetOtpPage(@RequestParam("email") String email, Model model) {
        if (!model.containsAttribute("verifyPasswordResetOtpForm")) {
            VerifyPasswordResetOtpForm form = new VerifyPasswordResetOtpForm();
            form.setEmail(email);
            model.addAttribute("verifyPasswordResetOtpForm", form);
        }
        model.addAttribute("email", email);
        return "auth/password-recovery-verify";
    }

    @PostMapping("/forgot-password/verify")
    public String verifyPasswordResetOtp(@Valid @ModelAttribute("verifyPasswordResetOtpForm") VerifyPasswordResetOtpForm form,
                                         BindingResult bindingResult,
                                         HttpServletRequest request,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + form.getEmail());
                String sessionToken = authService.verifyPasswordResetOtp(form.getEmail(), form.getCode());
                redirectAttributes.addFlashAttribute("successMessage", "Xác thực OTP thành công. Vui lòng thiết lập mật khẩu mới.");
                return "redirect:/reset-password?email=" + form.getEmail().trim().toLowerCase() + "&token=" + sessionToken;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        model.addAttribute("email", form.getEmail());
        return "auth/password-recovery-verify";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("email") String email,
                                    @RequestParam("token") String token,
                                    Model model) {
        if (!model.containsAttribute("resetPasswordRequest")) {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail(email);
            request.setToken(token);
            model.addAttribute("resetPasswordRequest", request);
        }
        return "auth/password-reset-form";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                authService.resetPassword(
                        resetPasswordRequest.getEmail(),
                        resetPasswordRequest.getToken(),
                        resetPasswordRequest.getNewPassword(),
                        resetPasswordRequest.getConfirmPassword()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay.");
                return "redirect:/login";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        resetPasswordRequest.setNewPassword(null);
        resetPasswordRequest.setConfirmPassword(null);
        return "auth/password-reset-form";
    }

    // ======================== CHANGE PASSWORD (IN PROFILE) ========================

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                 @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                authService.changePassword(currentUser.getId(), form.getCurrentPassword(), form.getNewPassword(), form.getConfirmPassword());
                redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
                return "redirect:/profile";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        User user = userRepository.findById(currentUser.getId()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        if (!model.containsAttribute("profileEditForm")) {
            com.school.lessonplan.user.dto.ProfileEditForm editForm = new com.school.lessonplan.user.dto.ProfileEditForm();
            if (user != null) {
                editForm.setLastName(user.getLastName());
                editForm.setGivenName(user.getGivenName());
                if (user.getDepartment() != null) editForm.setDepartmentId(user.getDepartment().getId());
                if (user.getCampus() != null) editForm.setCampusId(user.getCampus().getId());
            }
            model.addAttribute("profileEditForm", editForm);
        }
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
        return "user/user-profile-view";
    }
}
