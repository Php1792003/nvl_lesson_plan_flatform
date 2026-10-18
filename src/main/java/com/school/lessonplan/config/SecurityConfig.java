package com.school.lessonplan.config;

import com.school.lessonplan.security.JwtAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
                // Session: STATELESS (dùng JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))

                // CSRF: bật cho mọi POST/PUT/PATCH/DELETE, token trong cookie cho Thymeleaf đọc
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .ignoringRequestMatchers("/auth/refresh")
                )

                // Headers bảo mật
                .headers(headers -> {
                        headers.frameOptions(frame -> frame.sameOrigin());
                        headers.contentTypeOptions(contentType -> {});
                        headers.contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'"));
                        headers.referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                        headers.permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"));
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1 năm
                        );
                })

                // Phân quyền URL
                .authorizeHttpRequests(auth -> auth
                        // Public pages
                        .requestMatchers(
                                "/login", "/register", "/verify-email", "/verify-email/resend",
                                "/forgot-password", "/forgot-password/verify", "/reset-password",
                                "/css/**", "/js/**", "/img/**", "/vendor/**", "/images/**", "/favicon.ico",
                                "/.well-known/**", "/error", "/error/**")
                        .permitAll()
                        // API auth endpoints
                        .requestMatchers("/auth/**").permitAll()
                        // Admin routes
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // API endpoints
                        .requestMatchers("/api/dashboard/**").authenticated()
                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // Custom login page (không dùng form login mặc định của Spring)
                .formLogin(form -> form.disable())

                // Logout handled manually
                .logout(logout -> logout.disable())

                // Exception handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Auth entry point for [{} {}]: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());
                            response.sendRedirect(request.getContextPath() + "/login");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Access denied for [{} {}]: {}", request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
                            response.sendRedirect(request.getContextPath() + "/error/403");
                        }))

                // JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
