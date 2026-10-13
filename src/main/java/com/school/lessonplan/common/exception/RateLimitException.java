package com.school.lessonplan.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = -1; // không xác định
    }

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Thời gian (giây) client nên đợi trước khi thử lại.
     * Trả -1 nếu không xác định được.
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
