package com.school.lessonplan.nq52.signature;

/**
 * Ngoại lệ xác thực chữ ký số kèm nội dung phản hồi thô (raw JSON response) từ Cổng API Nhà phát hành.
 */
public class CaAuthenticationException extends IllegalArgumentException {
    private final String rawResponse;

    public CaAuthenticationException(String message) {
        super(message);
        this.rawResponse = null;
    }

    public CaAuthenticationException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
