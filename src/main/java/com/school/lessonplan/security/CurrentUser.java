package com.school.lessonplan.security;

import com.school.lessonplan.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

/**
 * Đối tượng chứa thông tin user đăng nhập hiện tại trong SecurityContext.
 * Quyền (authorities) được tải từ DB, không tin JWT.
 */
public class CurrentUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public CurrentUser(User user, Set<GrantedAuthority> authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.getStatus() == com.school.lessonplan.user.UserStatus.ACTIVE;
        this.authorities = authorities;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return enabled; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
