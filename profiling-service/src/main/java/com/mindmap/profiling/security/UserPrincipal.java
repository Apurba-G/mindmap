package com.mindmap.profiling.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetails implementation holding authenticated user context.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String handle;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String handle, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.handle = handle != null ? handle : "user-" + (userId != null ? userId.toString().substring(0, 8) : "anonymous");
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userId != null ? userId.toString() : "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Checks if the principal holds the ADMIN role.
     *
     * @return true if admin, false otherwise
     */
    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }
}
