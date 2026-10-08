package com.school.lessonplan.security;

import com.school.lessonplan.role.Permission;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT Authentication Filter.
 * Mỗi request: kiểm tra cookie → parse JWT → validate chữ ký + hạn
 * → kiểm tra user status == ACTIVE → kiểm tra token_version
 * → tải quyền từ DB → set SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final CookieService cookieService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, CookieService cookieService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String accessToken = cookieService.getAccessToken(request);

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                Jws<Claims> jws = jwtService.parseAccessToken(accessToken);
                Claims claims = jws.getPayload();

                Long userId = Long.parseLong(claims.getSubject());
                int tokenVersion = claims.get("tv", Integer.class);

                // Tải user từ DB - kiểm tra status và token_version
                User user = userRepository.findByIdWithRolesAndPermissions(userId).orElse(null);
                if (user != null
                        && user.getStatus() == UserStatus.ACTIVE
                        && user.getTokenVersion() == tokenVersion) {

                    // Tải quyền từ DB (không tin JWT)
                    Set<GrantedAuthority> authorities = loadAuthorities(user);
                    CurrentUser currentUser = new CurrentUser(user, authorities);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(currentUser,
                            null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                // Token không hợp lệ → không set context → user = anonymous
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Tải tất cả permission codes từ roles của user.
     * Quyền tải từ DB, cache ngắn có thể thêm sau.
     */
    private Set<GrantedAuthority> loadAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            // Thêm role code dạng ROLE_ADMIN, ROLE_TEACHER
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            // Thêm tất cả permission codes
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        return authorities;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/")
                || path.startsWith("/vendor/")
                || path.startsWith("/images/") || path.equals("/favicon.ico")
                || path.startsWith("/.well-known/");
    }
}
