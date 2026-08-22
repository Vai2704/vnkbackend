package com.example.vnkapp.security;

import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.enums.admin.AdminRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Principal stored in {@link org.springframework.security.core.context.SecurityContext}
 * after successful admin session-token validation.
 */
@Getter
public class AuthenticatedAdmin implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final AdminRole role;
    private final boolean enabled;

    public AuthenticatedAdmin(Admin admin) {
        this.id = admin.getId();
        this.username = admin.getUsername();
        this.email = admin.getEmail();
        this.role = admin.getRole() != null ? admin.getRole() : AdminRole.ADMIN;
        this.enabled = admin.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
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
        return enabled;
    }
}
