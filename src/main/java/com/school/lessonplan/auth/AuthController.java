package com.school.lessonplan.auth;

import com.school.lessonplan.auth.dto.LoginRequest;
import com.school.lessonplan.auth.dto.RegisterRequest;
import com.school.lessonplan.auth.dto.ResetPasswordRequest;
import com.school.lessonplan.auth.dto.VerifyOtpRequest;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
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

    public AuthController(AuthService authService,
                          DepartmentRepository departmentRepository,
                          CampusRepository campusRepository,
                          UserRepository userRepository,
                          RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
    }

    // ======================== LOGIN ========================

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser != null) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            rateLimiterService.checkLoginRateLimit(request.getRemoteAddr() + ":" + loginRequest.getEmail());
            authService.login(loginRequest, request, response);
            return "redirect:/dashboard";
        } catch (BusinessException | RateLimitException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("loginRequest", loginRequest);
            return "redirect:/login";
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
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("campuses", campusRepository.findAll());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("campuses", campusRepository.findAll());
            return "auth/register";
        }

        try {
            rateLimiterService.checkRegisterRateLimit(request.getRemoteAddr());
            User user = authService.register(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đăng ký thành công! Vui lòng nhập mã xác thực OTP đã được gửi đến email của bạn.");
            return "redirect:/verify-email?userId=" + user.getId();
        } catch (BusinessException | RateLimitException e) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("campuses", campusRepository.findAll());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
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
        return "auth/verify-email";
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

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("userId", userId);
            return "auth/verify-email";
        }

        try {
            rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + user.getEmail());
            authService.verifyEmailOtp(userId, verifyOtpRequest.getCode());
            redirectAttributes.addFlashAttribute("successMessage",
                "Xác thực email thành công! Tài khoản đang chờ Ban Giám Hiệu phê duyệt. Bạn sẽ nhận được thông báo qua email khi tài khoản được kích hoạt.");
            return "redirect:/login";
        } catch (BusinessException | RateLimitException e) {
            model.addAttribute("user", user);
            model.addAttribute("userId", userId);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/verify-email";
        }
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
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + email);
            authService.sendPasswordResetOtp(email);
            redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email tồn tại trong hệ thống, mã xác thực đã được gửi đến email của bạn.");
            return "redirect:/forgot-password/verify?email=" + email.trim().toLowerCase();
        } catch (RateLimitException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/forgot-password/verify")
    public String verifyPasswordResetOtpPage(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "auth/forgot-password-verify";
    }

    @PostMapping("/forgot-password/verify")
    public String verifyPasswordResetOtp(@RequestParam("email") String email,
                                         @RequestParam("code") String code,
                                         HttpServletRequest request,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            rateLimiterService.checkOtpRateLimit(request.getRemoteAddr() + ":" + email);
            String sessionToken = authService.verifyPasswordResetOtp(email, code);
            redirectAttributes.addFlashAttribute("successMessage", "Xác thực OTP thành công. Vui lòng thiết lập mật khẩu mới.");
            return "redirect:/reset-password?email=" + email.trim().toLowerCase() + "&token=" + sessionToken;
        } catch (BusinessException | RateLimitException e) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password-verify";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("email") String email,
                                    @RequestParam("token") String token,
                                    Model model) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setToken(token);
        model.addAttribute("resetPasswordRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            authService.resetPassword(
                resetPasswordRequest.getEmail(),
                resetPasswordRequest.getToken(),
                resetPasswordRequest.getNewPassword(),
                resetPasswordRequest.getConfirmPassword()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }

    // ======================== CHANGE PASSWORD (IN PROFILE) ========================

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            authService.changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }
}
