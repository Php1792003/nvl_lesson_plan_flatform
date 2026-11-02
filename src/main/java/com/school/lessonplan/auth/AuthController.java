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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private final AuthService authService;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;
    private final RateLimiterService rateLimiterService;
    private final com.school.lessonplan.security.CookieService cookieService;
    private final com.school.lessonplan.security.ActiveUserService activeUserService;

    public AuthController(AuthService authService,
                          DepartmentRepository departmentRepository,
                          CampusRepository campusRepository,
                          UserRepository userRepository,
                          RateLimiterService rateLimiterService,
                          com.school.lessonplan.security.CookieService cookieService,
                          com.school.lessonplan.security.ActiveUserService activeUserService) {
        this.authService = authService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
        this.cookieService = cookieService;
        this.activeUserService = activeUserService;
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
            rateLimiterService.checkLoginRateLimit(getClientIp(request) + ":" + loginRequest.getEmail());
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
    public String logout(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request, HttpServletResponse response) {
        if (currentUser != null) {
            activeUserService.removeUser(currentUser.getId());
        }
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
                rateLimiterService.checkRegisterRateLimit(getClientIp(request));
                PendingRegistration pending = authService.register(registerRequest);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đăng ký thành công! Vui lòng nhập mã xác thực OTP đã được gửi đến email của bạn.");
                return "redirect:/verify-email?email=" + java.net.URLEncoder.encode(pending.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject(null, e.getMessage());
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

    // ======================== TERMS & PRIVACY ========================

    @GetMapping("/terms-and-privacy")
    public String termsAndPrivacyPage(@RequestParam(name = "tab", required = false, defaultValue = "all") String tab,
                                      Model model) {
        model.addAttribute("activeTab", tab);
        return "auth/terms-and-privacy";
    }

    @GetMapping("/terms")
    public String termsPage() {
        return "redirect:/terms-and-privacy?tab=terms";
    }

    @GetMapping("/privacy")
    public String privacyPage() {
        return "redirect:/terms-and-privacy?tab=privacy";
    }

    // ======================== EMAIL OTP VERIFICATION ========================

    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam(name = "email", required = false) String email,
                                  @RequestParam(name = "userId", required = false) Long userId,
                                  Model model) {
        String targetEmail = email;
        if ((targetEmail == null || targetEmail.isBlank()) && userId != null) {
            User legacyUser = userRepository.findById(userId).orElse(null);
            if (legacyUser != null) {
                targetEmail = legacyUser.getEmail();
            }
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return "redirect:/login";
        }

        targetEmail = targetEmail.trim().toLowerCase();

        // Kiểm tra xem đã có tài khoản chính thức trong users chưa
        User existingUser = userRepository.findByEmail(targetEmail).orElse(null);
        if (existingUser != null && existingUser.getStatus() != com.school.lessonplan.user.UserStatus.PENDING_EMAIL) {
            return "redirect:/login";
        }

        var pendingOpt = authService.getPendingRegistration(targetEmail);
        if (pendingOpt.isEmpty() && existingUser == null) {
            return "redirect:/register";
        }

        model.addAttribute("email", targetEmail);
        model.addAttribute("userId", userId);
        if (!model.containsAttribute("verifyOtpRequest")) {
            model.addAttribute("verifyOtpRequest", new VerifyOtpRequest());
        }
        return "auth/email-verification-page";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam(name = "email", required = false) String email,
                              @RequestParam(name = "userId", required = false) Long userId,
                              @Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest verifyOtpRequest,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        String targetEmail = email;
        if ((targetEmail == null || targetEmail.isBlank()) && userId != null) {
            User legacyUser = userRepository.findById(userId).orElse(null);
            if (legacyUser != null) {
                targetEmail = legacyUser.getEmail();
            }
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return "redirect:/login";
        }

        targetEmail = targetEmail.trim().toLowerCase();

        if (!bindingResult.hasErrors()) {
            try {
                rateLimiterService.checkOtpRateLimit(getClientIp(request) + ":" + targetEmail);
                if (authService.hasPendingRegistration(targetEmail)) {
                    authService.verifyRegistrationOtp(targetEmail, verifyOtpRequest.getCode());
                } else if (userId != null) {
                    authService.verifyEmailOtp(userId, verifyOtpRequest.getCode());
                } else {
                    throw new BusinessException("Phiên đăng ký không tồn tại hoặc đã hết hạn. Vui lòng đăng ký lại.");
                }

                redirectAttributes.addFlashAttribute("successMessage",
                        "Xác thực email thành công! Tài khoản đang chờ Ban Giám Hiệu phê duyệt. Bạn sẽ nhận được thông báo qua email khi tài khoản được kích hoạt.");
                return "redirect:/login";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject(null, e.getMessage());
            }
        }

        model.addAttribute("email", targetEmail);
        model.addAttribute("userId", userId);
        return "auth/email-verification-page";
    }

    @PostMapping("/verify-email/resend")
    public String resendEmailOtp(@RequestParam(name = "email", required = false) String email,
                                 @RequestParam(name = "userId", required = false) Long userId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String targetEmail = email;
        if ((targetEmail == null || targetEmail.isBlank()) && userId != null) {
            User legacyUser = userRepository.findById(userId).orElse(null);
            if (legacyUser != null) {
                targetEmail = legacyUser.getEmail();
            }
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return "redirect:/login";
        }

        targetEmail = targetEmail.trim().toLowerCase();

        try {
            rateLimiterService.checkOtpRateLimit(getClientIp(request) + ":" + targetEmail);
            if (authService.hasPendingRegistration(targetEmail)) {
                authService.resendRegistrationOtp(targetEmail);
            } else if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    authService.sendVerificationOtp(user);
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Mã xác thực mới đã được gửi vào email của bạn.");
        } catch (BusinessException | RateLimitException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/verify-email?email=" + java.net.URLEncoder.encode(targetEmail, java.nio.charset.StandardCharsets.UTF_8);
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
            rateLimiterService.checkOtpRateLimit(getClientIp(request) + ":" + forgotPasswordForm.getEmail());
            authService.sendPasswordResetOtp(forgotPasswordForm.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Nếu email tồn tại trong hệ thống, mã xác thực đã được gửi đến email của bạn.");
            return "redirect:/forgot-password/verify?email=" + forgotPasswordForm.getEmail().trim().toLowerCase();
        } catch (RateLimitException e) {
            bindingResult.reject(null, e.getMessage());
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
                rateLimiterService.checkOtpRateLimit(getClientIp(request) + ":" + form.getEmail());
                String sessionToken = authService.verifyPasswordResetOtp(form.getEmail(), form.getCode());
                redirectAttributes.addFlashAttribute("successMessage", "Xác thực OTP thành công. Vui lòng thiết lập mật khẩu mới.");
                return "redirect:/reset-password?email=" + form.getEmail().trim().toLowerCase() + "&token=" + sessionToken;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException | RateLimitException e) {
                bindingResult.reject(null, e.getMessage());
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
                bindingResult.reject(null, e.getMessage());
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
                bindingResult.reject(null, e.getMessage());
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
