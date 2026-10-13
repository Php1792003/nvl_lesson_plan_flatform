package com.school.lessonplan.security;

import com.school.lessonplan.common.exception.RateLimitException;
import com.school.lessonplan.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final AppProperties appProperties;
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> otpBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> uploadBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void checkLoginRateLimit(String key) {
        Bucket bucket = loginBuckets.computeIfAbsent(key, k -> createBucket(
            appProperties.getRateLimit().getLogin().getCapacity(),
            appProperties.getRateLimit().getLogin().getRefillMinutes()
        ));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau.");
        }
    }

    public void checkRegisterRateLimit(String key) {
        Bucket bucket = registerBuckets.computeIfAbsent(key, k -> createBucket(
            appProperties.getRateLimit().getRegister().getCapacity(),
            appProperties.getRateLimit().getRegister().getRefillMinutes()
        ));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Quá nhiều yêu cầu đăng ký từ địa chỉ của bạn. Vui lòng thử lại sau.");
        }
    }

    public void checkOtpRateLimit(String key) {
        Bucket bucket = otpBuckets.computeIfAbsent(key, k -> createBucket(
            appProperties.getRateLimit().getOtp().getCapacity(),
            appProperties.getRateLimit().getOtp().getRefillMinutes()
        ));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Quá nhiều yêu cầu gửi mã xác thực. Vui lòng thử lại sau.");
        }
    }

    /**
     * Kiểm tra Token Bucket upload theo tài khoản đã xác thực (userId từ server).
     * - Capacity: 3 token (mặc định)
     * - Refill: 1 token mỗi 10 giây (intervally - đều đặn)
     * - Mỗi yêu cầu upload tiêu thụ 1 token.
     *
     * @param userId ID người dùng đã xác thực từ SecurityContext (không phải client tự khai)
     * @throws RateLimitException nếu vượt hạn mức, kèm retryAfterSeconds
     */
    public void checkUploadRateLimit(Long userId) {
        AppProperties.UploadRateLimitConfig cfg = appProperties.getRateLimit().getUpload();
        Bucket bucket = uploadBuckets.computeIfAbsent(userId, k -> createUploadBucket(cfg));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            log.warn("Upload rate limit exceeded for userId={}, retryAfter={}s", userId, retryAfterSeconds);
            throw new RateLimitException(
                "Bạn đang tải lên quá nhanh. Vui lòng đợi " + retryAfterSeconds + " giây rồi thử lại.",
                retryAfterSeconds
            );
        }
    }

    @SuppressWarnings("deprecation")
    private Bucket createBucket(int capacity, int refillMinutes) {
        Bandwidth limit = Bandwidth.classic(
            capacity,
            Refill.greedy(capacity, Duration.ofMinutes(refillMinutes))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Token Bucket cho upload: bổ sung 1 token mỗi refillSeconds giây,
     * tối đa capacity token trong bucket.
     */
    @SuppressWarnings("deprecation")
    private Bucket createUploadBucket(AppProperties.UploadRateLimitConfig cfg) {
        Bandwidth limit = Bandwidth.classic(
            cfg.getCapacity(),
            Refill.intervally(1, Duration.ofSeconds(cfg.getRefillSeconds()))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
