package com.school.lessonplan.auth;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.auth.dto.LoginRequest;
import com.school.lessonplan.auth.dto.RegisterRequest;
import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.RateLimitException;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.mail.EmailService;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.security.CookieService;
import com.school.lessonplan.security.JwtService;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final EmailVerificationCodeRepository otpRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
            DepartmentRepository departmentRepository, CampusRepository campusRepository,
            EmailVerificationCodeRepository otpRepository,
            PasswordResetTokenRepository resetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            CookieService cookieService, AuditService auditService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.otpRepository = otpRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    // ======================== ĐĂNG KÝ ========================

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Kiểm tra mật khẩu khớp
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp.");
        }

        // Kiểm tra mật khẩu có chữ và số
        if (!request.getPassword().matches(".*[a-zA-Z].*") || !request.getPassword().matches(".*\\d.*")) {
            throw new BusinessException("Mật khẩu phải chứa cả chữ và số.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));

        Campus campus;
        if (request.getCampusId() != null) {
            campus = campusRepository.findById(request.getCampusId())
                    .orElseThrow(() -> new BusinessException("Phân hiệu không tồn tại."));
        } else {
            campus = campusRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phân hiệu mặc định."));
        }

        // Kiểm tra email trùng hoặc tài khoản chưa xác thực OTP
        User user;
        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (existing.getStatus() == UserStatus.PENDING_EMAIL) {
                // Người dùng trước đó chưa xác thực OTP -> cho phép cập nhật thông tin và cấp mã mới
                user = existing;
                otpRepository.deleteByUserId(user.getId());
            } else {
                throw new BusinessException("Email đã được sử dụng.");
            }
        } else {
            user = new User();
            user.setEmail(email);
            // Gán role TEACHER
            Role teacherRole = roleRepository.findByCode("TEACHER")
                    .orElseThrow(() -> new BusinessException("Vai trò TEACHER chưa được tạo."));
            user.getRoles().add(teacherRole);
        }

        user.setLastName(request.getLastName().trim());
        user.setGivenName(request.getGivenName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(department);
        user.setCampus(campus);
        user.setStatus(UserStatus.PENDING_EMAIL);

        user = userRepository.save(user);

        // Gửi mã OTP
        sendVerificationOtp(user);

        auditService.log("USER_REGISTER", "USER", user.getId(), "Đăng ký tài khoản: " + email);

        return user;
    }

    // ======================== OTP XÁC THỰC EMAIL ========================

    @Transactional
    public void sendVerificationOtp(User user) {
        // Kiểm tra rate limit: tối đa 5 lần/giờ
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = otpRepository.countRecentByUserId(user.getId(), oneHourAgo);
        if (recentCount >= appProperties.getOtp().getMaxResendPerHour()) {
            throw new RateLimitException("Bạn đã gửi quá nhiều mã xác thực. Vui lòng thử lại sau.");
        }

        // Kiểm tra cooldown 60 giây
        var activeCodes = otpRepository.findActiveByUserId(user.getId());
        if (!activeCodes.isEmpty()) {
            EmailVerificationCode lastCode = activeCodes.get(0);
            Instant cooldownEnd = lastCode.getLastSentAt()
                    .plus(appProperties.getOtp().getResendCooldownSeconds(), ChronoUnit.SECONDS);
            if (Instant.now().isBefore(cooldownEnd)) {
                throw new RateLimitException("Vui lòng đợi 60 giây trước khi gửi lại mã.");
            }
        }

        // Sinh mã 6 chữ số
        String code = generateOtpCode();
        String codeHash = hashSha256(code);

        EmailVerificationCode otp = new EmailVerificationCode();
        otp.setUser(user);
        otp.setCodeHash(codeHash);
        otp.setExpiresAt(Instant.now().plus(appProperties.getOtp().getExpiryMinutes(), ChronoUnit.MINUTES));
        otp.setLastSentAt(Instant.now());
        otpRepository.save(otp);

        // Thêm email vào outbox
        emailService.sendOtpVerification(user.getEmail(), user.getFullName(), code);

        log.info("OTP sent to user {}", user.getId());
    }

    @Transactional
    public boolean verifyEmailOtp(Long userId, String code) {
        var activeCodes = otpRepository.findActiveByUserId(userId);
        if (activeCodes.isEmpty()) {
            throw new BusinessException("Không tìm thấy mã xác thực. Vui lòng yêu cầu gửi lại.");
        }

        EmailVerificationCode otp = activeCodes.get(0);

        // Kiểm tra đã dùng chưa
        if (otp.isConsumed()) {
            throw new BusinessException("Mã đã được sử dụng. Vui lòng yêu cầu mã mới.");
        }

        // Kiểm tra hết hạn
        if (otp.isExpired()) {
            throw new BusinessException("Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Kiểm tra số lần nhập sai
        if (otp.isMaxAttemptsReached(appProperties.getOtp().getMaxAttempts())) {
            throw new BusinessException(
                    "Đã nhập sai quá " + appProperties.getOtp().getMaxAttempts() + " lần. Vui lòng yêu cầu mã mới.");
        }

        // Kiểm tra mã
        String inputHash = hashSha256(code.trim());
        if (!inputHash.equals(otp.getCodeHash())) {
            otp.incrementAttempts();
            otpRepository.save(otp);
            throw new BusinessException("Mã xác thực không đúng.");
        }

        // Thành công - đánh dấu đã dùng
        otp.setConsumedAt(Instant.now());
        otpRepository.save(otp);

        // Chuyển trạng thái user
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getStatus() == UserStatus.PENDING_EMAIL) {
            user.setStatus(UserStatus.PENDING_APPROVAL);
            userRepository.save(user);
        }

        return true;
    }

    // ======================== ĐĂNG NHẬP ========================

    @Transactional
    public User login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        // Thông báo chung chung - không lộ email có tồn tại
        if (user == null) {
            throw new BusinessException("Email hoặc mật khẩu không đúng.");
        }

        // Kiểm tra tạm khoá
        if (user.isTemporarilyLocked()) {
            throw new BusinessException("Tài khoản đang bị khoá tạm thời. Vui lòng thử lại sau.");
        }

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BusinessException("Email hoặc mật khẩu không đúng.");
        }

        // Kiểm tra trạng thái
        switch (user.getStatus()) {
            case PENDING_EMAIL ->
                throw new BusinessException("Tài khoản chưa xác thực email. Vui lòng kiểm tra email.");
            case PENDING_APPROVAL -> throw new BusinessException("Tài khoản đang chờ Ban Giám Hiệu duyệt.");
            case LOCKED -> throw new BusinessException("Tài khoản đã bị khoá. Lý do: " +
                    (user.getLockReason() != null ? user.getLockReason() : "Không rõ"));
            case ACTIVE -> {
            } // OK
        }

        // Reset failed count
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Tạo token pair
        issueTokens(user, httpRequest, httpResponse);

        auditService.log("USER_LOGIN", "USER", user.getId(), "Đăng nhập", httpRequest);

        return user;
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= appProperties.getSecurity().getMaxFailedLogin()) {
            user.setLockedUntil(Instant.now().plus(
                    appProperties.getSecurity().getLockDurationMinutes(), ChronoUnit.MINUTES));
            log.warn("User {} temporarily locked due to {} failed login attempts",
                    user.getId(), user.getFailedLoginCount());
        }
        userRepository.save(user);
    }

    // ======================== TOKEN ========================

    @Transactional
    public void issueTokens(User user, HttpServletRequest request, HttpServletResponse response) {
        // Access token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getTokenVersion());
        int accessMaxAge = appProperties.getJwt().getAccessTokenExpiryMinutes() * 60;
        cookieService.setAccessTokenCookie(response, accessToken, accessMaxAge);

        // Refresh token
        String refreshTokenValue = jwtService.generateRefreshTokenValue();
        String refreshHash = hashSha256(refreshTokenValue);
        String familyId = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(refreshHash);
        rt.setFamilyId(familyId);
        rt.setExpiresAt(jwtService.getRefreshTokenExpiry());
        rt.setIp(request != null ? request.getRemoteAddr() : null);
        rt.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        refreshTokenRepository.save(rt);

        int refreshMaxAge = appProperties.getJwt().getRefreshTokenExpiryDays() * 24 * 60 * 60;
        cookieService.setRefreshTokenCookie(response, refreshTokenValue, refreshMaxAge);
    }

    @Transactional
    public boolean refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.getRefreshToken(request);
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            return false;
        }

        String tokenHash = hashSha256(refreshTokenValue);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

        if (existingToken == null) {
            return false;
        }

        // Reuse detection: nếu token đã bị thu hồi → thu hồi cả family
        if (existingToken.isRevoked()) {
            log.warn("Refresh token reuse detected for family {}! Revoking entire family.",
                    existingToken.getFamilyId());
            refreshTokenRepository.revokeFamily(existingToken.getFamilyId());
            cookieService.clearAuthCookies(response);
            return false;
        }

        if (existingToken.isExpired()) {
            return false;
        }

        User user = existingToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }

        // Rotation: thu hồi token cũ, tạo mới
        existingToken.setRevokedAt(Instant.now());

        // Tạo refresh token mới trong cùng family
        String newRefreshValue = jwtService.generateRefreshTokenValue();
        String newRefreshHash = hashSha256(newRefreshValue);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setTokenHash(newRefreshHash);
        newToken.setFamilyId(existingToken.getFamilyId());
        newToken.setExpiresAt(jwtService.getRefreshTokenExpiry());
        newToken.setIp(request.getRemoteAddr());
        newToken.setUserAgent(request.getHeader("User-Agent"));
        newToken = refreshTokenRepository.save(newToken);

        existingToken.setReplacedById(newToken.getId());
        refreshTokenRepository.save(existingToken);

        // Issue new access token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getTokenVersion());
        int accessMaxAge = appProperties.getJwt().getAccessTokenExpiryMinutes() * 60;
        cookieService.setAccessTokenCookie(response, accessToken, accessMaxAge);

        int refreshMaxAge = appProperties.getJwt().getRefreshTokenExpiryDays() * 24 * 60 * 60;
        cookieService.setRefreshTokenCookie(response, newRefreshValue, refreshMaxAge);

        return true;
    }

    // ======================== ĐĂNG XUẤT ========================

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.getRefreshToken(request);
        if (refreshTokenValue != null) {
            String hash = hashSha256(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            });
        }
        cookieService.clearAuthCookies(response);
    }

    // ======================== QUÊN MẬT KHẨU (OTP) ========================

    /**
     * Bước 1: Gửi OTP qua email
     */
    @Transactional
    public void sendPasswordResetOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        // Không tiết lộ email có tồn tại hay không
        if (user == null) {
            log.debug("Password reset requested for non-existing email");
            return; // Im lặng, không throw exception
        }

        String code = generateOtpCode();
        String codeHash = hashSha256(code);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(codeHash);
        token.setExpiresAt(Instant.now().plus(appProperties.getOtp().getExpiryMinutes(), ChronoUnit.MINUTES));
        resetTokenRepository.save(token);

        emailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), code);
    }

    /**
     * Bước 2: Xác thực OTP quên mật khẩu, trả về session token
     */
    @Transactional
    public String verifyPasswordResetOtp(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Email hoặc mã xác thực không đúng."));

        var tokens = resetTokenRepository.findActiveByUserId(user.getId());
        if (tokens.isEmpty()) {
            throw new BusinessException("Không tìm thấy mã xác thực. Vui lòng yêu cầu gửi lại.");
        }

        PasswordResetToken resetToken = tokens.get(0);

        if (resetToken.isConsumed()) {
            throw new BusinessException("Mã đã được sử dụng.");
        }

        if (resetToken.isExpired()) {
            throw new BusinessException("Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (resetToken.isMaxAttemptsReached(appProperties.getOtp().getMaxAttempts())) {
            throw new BusinessException("Đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.");
        }

        String inputHash = hashSha256(code.trim());
        if (!inputHash.equals(resetToken.getCodeHash())) {
            resetToken.incrementAttempts();
            resetTokenRepository.save(resetToken);
            throw new BusinessException("Mã xác thực không đúng.");
        }

        // OTP đúng — đánh dấu consumed và tạo session token
        resetToken.setConsumedAt(Instant.now());
        resetTokenRepository.save(resetToken);

        // Tạo session token tạm thời để cho phép đổi mật khẩu
        String sessionToken = UUID.randomUUID().toString();
        // Lưu session token vào password_reset_tokens mới (dùng làm cầu nối)
        PasswordResetToken sessionEntry = new PasswordResetToken();
        sessionEntry.setUser(user);
        sessionEntry.setCodeHash(hashSha256(sessionToken));
        sessionEntry.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES)); // 10 phút để đổi mật khẩu
        resetTokenRepository.save(sessionEntry);

        return sessionToken;
    }

    /**
     * Bước 3: Đổi mật khẩu sau khi verify OTP thành công
     */
    @Transactional
    public void resetPassword(String email, String sessionToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu xác nhận không khớp.");
        }

        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException("Mật khẩu phải chứa cả chữ và số.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản."));

        // Kiểm tra session token
        String tokenHash = hashSha256(sessionToken);
        var tokens = resetTokenRepository.findActiveByUserId(user.getId());
        boolean validSession = tokens.stream()
                .anyMatch(t -> t.getCodeHash().equals(tokenHash) && !t.isExpired() && !t.isConsumed());

        if (!validSession) {
            throw new BusinessException("Phiên đặt lại mật khẩu đã hết hạn. Vui lòng thực hiện lại.");
        }

        // Đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion(); // Vô hiệu hoá tất cả token cũ
        userRepository.save(user);

        // Đánh dấu session token đã dùng
        tokens.stream()
                .filter(t -> t.getCodeHash().equals(tokenHash))
                .forEach(t -> {
                    t.setConsumedAt(Instant.now());
                    resetTokenRepository.save(t);
                });

        auditService.log("PASSWORD_RESET", "USER", user.getId(), "Đặt lại mật khẩu thành công");
    }

    // ======================== ĐỔI MẬT KHẨU (đã đăng nhập) ========================

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu xác nhận không khớp.");
        }

        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException("Mật khẩu phải chứa cả chữ và số.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        userRepository.save(user);

        auditService.log("PASSWORD_CHANGE", "USER", user.getId(), "Đổi mật khẩu");
    }

    // ======================== HELPERS ========================

    private String generateOtpCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(code);
    }

    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hash failed", e);
        }
    }
}
