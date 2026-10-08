package com.school.lessonplan.security;

import com.school.lessonplan.common.exception.RateLimitException;
import com.school.lessonplan.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final AppProperties appProperties;
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> otpBuckets = new ConcurrentHashMap<>();

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

    @SuppressWarnings("deprecation")
    private Bucket createBucket(int capacity, int refillMinutes) {
        Bandwidth limit = Bandwidth.classic(
            capacity,
            Refill.greedy(capacity, Duration.ofMinutes(refillMinutes))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
