package com.example.vnkapp.security;

import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.entity.AdminSession;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.entity.UserSession;
import com.example.vnkapp.repository.AdminRepository;
import com.example.vnkapp.repository.AdminSessionRepository;
import com.example.vnkapp.repository.UserRepository;
import com.example.vnkapp.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Validates {@code Authorization: Bearer <sessionToken>} against {@link UserSession}
 * or {@link com.example.vnkapp.entity.AdminSession} and populates
 * {@link org.springframework.security.core.context.SecurityContext}.
 */
@Component
public class SessionTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserSessionRepository userSessionRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public SessionTokenAuthenticationFilter(
            UserSessionRepository userSessionRepository,
            AdminSessionRepository adminSessionRepository,
            UserRepository userRepository,
            AdminRepository adminRepository) {
        this.userSessionRepository = userSessionRepository;
        this.adminSessionRepository = adminSessionRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authenticateUserSession(token) || authenticateAdminSession(token)) {
            // authenticated
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean authenticateUserSession(String token) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(token);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive() || sessionOpt.get().isExpired()) {
            return false;
        }

        UserSession session = sessionOpt.get();
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            return false;
        }

        AuthenticatedUser principal = new AuthenticatedUser(userOpt.get());
        setAuthentication(principal, session);
        return true;
    }

    private boolean authenticateAdminSession(String token) {
        Optional<AdminSession> sessionOpt = adminSessionRepository.findBySessionToken(token);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive() || sessionOpt.get().isExpired()) {
            return false;
        }

        AdminSession session = sessionOpt.get();
        Optional<Admin> adminOpt = adminRepository.findById(session.getAdminId());
        if (adminOpt.isEmpty() || !adminOpt.get().isActive()) {
            return false;
        }

        AuthenticatedAdmin principal = new AuthenticatedAdmin(adminOpt.get());
        setAuthentication(principal, session);
        return true;
    }

    private void setAuthentication(Object principal, Object sessionDetails) {
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser, null, authenticatedUser.getAuthorities());
            authentication.setDetails(sessionDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return;
        }

        if (principal instanceof AuthenticatedAdmin authenticatedAdmin) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedAdmin, null, authenticatedAdmin.getAuthorities());
            authentication.setDetails(sessionDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
