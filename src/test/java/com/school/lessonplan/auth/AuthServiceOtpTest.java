package com.school.lessonplan.auth;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.mail.EmailService;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.security.CookieService;
import com.school.lessonplan.security.JwtService;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceOtpTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private EmailVerificationCodeRepository otpRepository;
    @Mock private PendingRegistrationRepository pendingRegistrationRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CookieService cookieService;
    @Mock private AuditService auditService;

    private AuthService authService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        authService = new AuthService(
            userRepository, roleRepository, departmentRepository, campusRepository,
            otpRepository, pendingRegistrationRepository, resetTokenRepository, refreshTokenRepository, emailService,
            passwordEncoder, jwtService, cookieService, auditService, appProperties
        );
    }

    @Test
    @DisplayName("Quy tắc 4.2: Nhập đúng OTP sẽ chuyển trạng thái sang PENDING_APPROVAL")
    void verifyOtpSuccess() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.PENDING_EMAIL);

        EmailVerificationCode code = new EmailVerificationCode();
        code.setUser(user);
        code.setCodeHash(AuthService.hashSha256("123456"));
        code.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(otpRepository.findActiveByUserId(1L)).thenReturn(List.of(code));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = authService.verifyEmailOtp(1L, "123456");

        assertTrue(result);
        assertNotNull(code.getConsumedAt());
        assertEquals(UserStatus.PENDING_APPROVAL, user.getStatus());
        verify(otpRepository).save(code);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Quy tắc 4.2: OTP đã hết hạn thì từ chối")
    void verifyOtpExpired() {
        User user = new User();
        user.setId(1L);

        EmailVerificationCode code = new EmailVerificationCode();
        code.setUser(user);
        code.setCodeHash(AuthService.hashSha256("123456"));
        code.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(otpRepository.findActiveByUserId(1L)).thenReturn(List.of(code));

        BusinessException ex = assertThrows(BusinessException.class, () ->
            authService.verifyEmailOtp(1L, "123456")
        );
        assertTrue(ex.getMessage().contains("hết hạn"));
    }

    @Test
    @DisplayName("Quy tắc 4.2: Nhập sai OTP quá 5 lần thì khoá mã OTP đó")
    void verifyOtpMaxAttemptsExceeded() {
        User user = new User();
        user.setId(1L);

        EmailVerificationCode code = new EmailVerificationCode();
        code.setUser(user);
        code.setCodeHash(AuthService.hashSha256("123456"));
        code.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        code.setAttempts(5);

        when(otpRepository.findActiveByUserId(1L)).thenReturn(List.of(code));

        BusinessException ex = assertThrows(BusinessException.class, () ->
            authService.verifyEmailOtp(1L, "123456")
        );
        assertTrue(ex.getMessage().contains("quá 5 lần"));
    }

    @Test
    @DisplayName("Quên mật khẩu: Xác thực OTP thành công trả về session token hợp lệ để đổi mật khẩu")
    void verifyPasswordResetOtpSuccess() {
        User user = new User();
        user.setId(2L);
        user.setEmail("forgot@school.edu.vn");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(AuthService.hashSha256("654321"));
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("forgot@school.edu.vn")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findActiveByUserId(2L)).thenReturn(List.of(token));

        String sessionToken = authService.verifyPasswordResetOtp("forgot@school.edu.vn", "654321");

        assertNotNull(sessionToken);
        assertNotNull(token.getConsumedAt());
        verify(resetTokenRepository, atLeastOnce()).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Quy tắc mới: Đăng ký KHÔNG insert vào users table mà lưu vào pending_registrations")
    void registerDoesNotInsertIntoUsersTable() {
        com.school.lessonplan.auth.dto.RegisterRequest req = new com.school.lessonplan.auth.dto.RegisterRequest();
        req.setEmail("teacher.test@school.edu.vn");
        req.setLastName("Nguyễn");
        req.setGivenName("Văn An");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setDepartmentId(1L);
        req.setCampusId(1L);
        req.setAgreeTerms(true);

        com.school.lessonplan.department.Department dept = new com.school.lessonplan.department.Department();
        dept.setId(1L);
        com.school.lessonplan.campus.Campus campus = new com.school.lessonplan.campus.Campus();
        campus.setId(1L);

        when(userRepository.findByEmail("teacher.test@school.edu.vn")).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));
        when(pendingRegistrationRepository.findByEmail("teacher.test@school.edu.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPass");
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(i -> i.getArgument(0));

        PendingRegistration result = authService.register(req);

        assertNotNull(result);
        assertEquals("teacher.test@school.edu.vn", result.getEmail());
        verify(pendingRegistrationRepository).save(any(PendingRegistration.class));
        verify(userRepository, never()).save(any(User.class)); // Tuyệt đối KHÔNG insert vào users!
        verify(emailService).sendOtpVerification(eq("teacher.test@school.edu.vn"), anyString(), anyString());
    }

    @Test
    @DisplayName("Quy tắc mới: Xác thực OTP thành công thì MỚI insert tài khoản vào users table và xoá pending")
    void verifyRegistrationOtpSuccess() {
        PendingRegistration pending = new PendingRegistration();
        pending.setEmail("teacher.test@school.edu.vn");
        pending.setLastName("Nguyễn");
        pending.setGivenName("Văn An");
        pending.setPasswordHash("hashedPass");
        pending.setDepartmentId(1L);
        pending.setCampusId(1L);
        pending.setCodeHash(AuthService.hashSha256("123456"));
        pending.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        com.school.lessonplan.department.Department dept = new com.school.lessonplan.department.Department();
        dept.setId(1L);
        com.school.lessonplan.campus.Campus campus = new com.school.lessonplan.campus.Campus();
        campus.setId(1L);
        com.school.lessonplan.role.Role teacherRole = new com.school.lessonplan.role.Role();
        teacherRole.setCode("TEACHER");

        when(pendingRegistrationRepository.findByEmail("teacher.test@school.edu.vn")).thenReturn(Optional.of(pending));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));
        when(roleRepository.findByCode("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(99L);
            return u;
        });

        User created = authService.verifyRegistrationOtp("teacher.test@school.edu.vn", "123456");

        assertNotNull(created);
        assertEquals("teacher.test@school.edu.vn", created.getEmail());
        assertEquals(UserStatus.PENDING_APPROVAL, created.getStatus());
        verify(userRepository, times(1)).save(any(User.class)); // Chỉ bây giờ mới insert vào users
        verify(pendingRegistrationRepository).delete(pending); // Đã xoá pending
    }
}
