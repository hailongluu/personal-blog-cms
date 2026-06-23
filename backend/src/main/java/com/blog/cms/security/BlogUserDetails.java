package com.blog.cms.security;

import com.blog.cms.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal — wraps our User entity.
 *
 * <p>Stores the role name as a granted authority prefixed with "ROLE_"
 * (Spring Security convention for {@code hasRole(...)} checks).
 */
@Getter
public class BlogUserDetails implements UserDetails {

    private final User user;
    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final String roleName;
    private final boolean active;

    public BlogUserDetails(User user) {
        this.user = user;
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.roleName = user.getRole().getName();
        this.active = Boolean.TRUE.equals(user.getIsActive());
    }

    public Long getId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && user.getDeletedAt() == null;
    }
}
