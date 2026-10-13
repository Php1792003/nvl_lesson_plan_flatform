package com.school.lessonplan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private Storage storage = new Storage();
    private Upload upload = new Upload();
    private int keepVersions = 3;
    private Jwt jwt = new Jwt();
    private Mail mail = new Mail();
    private Otp otp = new Otp();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();
    private InitialAdmin initialAdmin = new InitialAdmin();

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public int getKeepVersions() {
        return keepVersions;
    }

    public void setKeepVersions(int keepVersions) {
        this.keepVersions = keepVersions;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Otp getOtp() {
        return otp;
    }

    public void setOtp(Otp otp) {
        this.otp = otp;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public InitialAdmin getInitialAdmin() {
        return initialAdmin;
    }

    public void setInitialAdmin(InitialAdmin initialAdmin) {
        this.initialAdmin = initialAdmin;
    }

    public static class Storage {
        private String root = "./uploads";
        private String tempDir = "./uploads/temp";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }
    }

    public static class Upload {
        private int maxMb = 20;
        private String allowedExtensions = "pdf,doc,docx,ppt,pptx";
        private String allowedContentTypes = "";
        private int maxConcurrent = 3;
        private int minDiskMb = 500;

        public int getMaxMb() {
            return maxMb;
        }

        public void setMaxMb(int maxMb) {
            this.maxMb = maxMb;
        }

        public String getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(String allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public String getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(String allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }

        public int getMinDiskMb() {
            return minDiskMb;
        }

        public void setMinDiskMb(int minDiskMb) {
            this.minDiskMb = minDiskMb;
        }
    }

    public static class Jwt {
        private String secret;
        private int accessTokenExpiryMinutes = 15;
        private int refreshTokenExpiryDays = 7;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getAccessTokenExpiryMinutes() {
            return accessTokenExpiryMinutes;
        }

        public void setAccessTokenExpiryMinutes(int accessTokenExpiryMinutes) {
            this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        }

        public int getRefreshTokenExpiryDays() {
            return refreshTokenExpiryDays;
        }

        public void setRefreshTokenExpiryDays(int refreshTokenExpiryDays) {
            this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        }
    }

    public static class Mail {
        private String from = "noreply@nguyenvanlinh.edu.vn";
        private String senderName = "THCS Nguyễn Văn Linh";
        private boolean devMode = false;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public boolean isDevMode() {
            return devMode;
        }

        public void setDevMode(boolean devMode) {
            this.devMode = devMode;
        }
    }

    public static class Otp {
        private int expiryMinutes = 10;
        private int maxAttempts = 5;
        private int resendCooldownSeconds = 60;
        private int maxResendPerHour = 5;

        public int getExpiryMinutes() {
            return expiryMinutes;
        }

        public void setExpiryMinutes(int expiryMinutes) {
            this.expiryMinutes = expiryMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getResendCooldownSeconds() {
            return resendCooldownSeconds;
        }

        public void setResendCooldownSeconds(int resendCooldownSeconds) {
            this.resendCooldownSeconds = resendCooldownSeconds;
        }

        public int getMaxResendPerHour() {
            return maxResendPerHour;
        }

        public void setMaxResendPerHour(int maxResendPerHour) {
            this.maxResendPerHour = maxResendPerHour;
        }
    }

    public static class RateLimit {
        private RateLimitConfig login = new RateLimitConfig(10, 15);
        private RateLimitConfig register = new RateLimitConfig(5, 60);
        private RateLimitConfig otp = new RateLimitConfig(5, 60);
        private UploadRateLimitConfig upload = new UploadRateLimitConfig();

        public RateLimitConfig getLogin() {
            return login;
        }

        public void setLogin(RateLimitConfig login) {
            this.login = login;
        }

        public RateLimitConfig getRegister() {
            return register;
        }

        public void setRegister(RateLimitConfig register) {
            this.register = register;
        }

        public RateLimitConfig getOtp() {
            return otp;
        }

        public void setOtp(RateLimitConfig otp) {
            this.otp = otp;
        }

        public UploadRateLimitConfig getUpload() {
            return upload;
        }

        public void setUpload(UploadRateLimitConfig upload) {
            this.upload = upload;
        }
    }

    public static class RateLimitConfig {
        private int capacity;
        private int refillMinutes;

        public RateLimitConfig() {
        }

        public RateLimitConfig(int capacity, int refillMinutes) {
            this.capacity = capacity;
            this.refillMinutes = refillMinutes;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillMinutes() {
            return refillMinutes;
        }

        public void setRefillMinutes(int refillMinutes) {
            this.refillMinutes = refillMinutes;
        }
    }

    public static class Security {
        private int maxFailedLogin = 5;
        private int lockDurationMinutes = 30;

        public int getMaxFailedLogin() {
            return maxFailedLogin;
        }

        public void setMaxFailedLogin(int maxFailedLogin) {
            this.maxFailedLogin = maxFailedLogin;
        }

        public int getLockDurationMinutes() {
            return lockDurationMinutes;
        }

        public void setLockDurationMinutes(int lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }
    }

    /**
     * Cấu hình Token Bucket cho upload: capacity (số token tối đa) và
     * refillSeconds (mỗi bao nhiêu giây bổ sung 1 token).
     */
    public static class UploadRateLimitConfig {
        private int capacity = 3;
        private int refillSeconds = 10;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillSeconds() {
            return refillSeconds;
        }

        public void setRefillSeconds(int refillSeconds) {
            this.refillSeconds = refillSeconds;
        }
    }

    public static class InitialAdmin {
        private String email = "admin@school.edu.vn";
        private String password = "Admin@123456";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
