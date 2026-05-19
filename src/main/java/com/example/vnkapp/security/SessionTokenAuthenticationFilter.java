package com.example.vnkapp.security;

import com.example.vnkapp.entity.User;
import com.example.vnkapp.entity.UserSession;
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
 * and populates {@link org.springframework.security.core.context.SecurityContext}.
 */
@Component
public class SessionTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public SessionTokenAuthenticationFilter(
            UserSessionRepository userSessionRepository,
            UserRepository userRepository) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
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

        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(token);
        if (sessionOpt.isEmpty() || sessionOpt.get().isExpired()) {
            filterChain.doFilter(request, response);
            return;
        }

        UserSession session = sessionOpt.get();
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(userOpt.get());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(session);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
