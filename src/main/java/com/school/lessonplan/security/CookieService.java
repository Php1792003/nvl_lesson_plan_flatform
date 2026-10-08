package com.school.lessonplan.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Tạo cookie access token
     * HttpOnly; Secure; SameSite=Lax; Path=/
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        // SameSite=Lax set via response header since Cookie API doesn't support it
        response.addHeader("Set-Cookie", buildCookieHeader(ACCESS_TOKEN_COOKIE, token, "/", maxAgeSeconds));
    }

    /**
     * Tạo cookie refresh token
     * HttpOnly; Secure; SameSite=Lax; Path=/auth
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        response.addHeader("Set-Cookie", buildCookieHeader(REFRESH_TOKEN_COOKIE, token, "/auth", maxAgeSeconds));
    }

    /**
     * Xoá cả hai cookie (logout)
     */
    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookieHeader(ACCESS_TOKEN_COOKIE, "", "/", 0));
        response.addHeader("Set-Cookie", buildCookieHeader(REFRESH_TOKEN_COOKIE, "", "/auth", 0));
    }

    /**
     * Đọc access token từ cookie
     */
    public String getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Đọc refresh token từ cookie
     */
    public String getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String buildCookieHeader(String name, String value, String path, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=").append(path);
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; HttpOnly");
        if (isSecure()) {
            sb.append("; Secure");
        }
        sb.append("; SameSite=Lax");
        return sb.toString();
    }

    private boolean isSecure() {
        return serverPort == 443 || serverPort == 8443;
    }
}
