package com.example.vnkapp.service;

import com.example.vnkapp.dto.admin.AdminLoginRequestDto;
import com.example.vnkapp.dto.admin.AdminLoginResponseDto;
import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.entity.AdminSession;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.repository.AdminRepository;
import com.example.vnkapp.repository.AdminSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminRepository adminRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthService(
            AdminRepository adminRepository,
            AdminSessionRepository adminSessionRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.adminSessionRepository = adminSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminLoginResponseDto login(AdminLoginRequestDto request) {
        log.debug("Admin login attempt for username: {}", request.username());

        Admin admin = adminRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> {
                    log.warn("Admin login failed - user not found: {}", request.username());
                    return new IllegalArgumentException("Invalid username or password");
                });

        if (!admin.isActive()) {
            log.warn("Admin login failed - inactive account: {}", request.username());
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            log.warn("Admin login failed - wrong password for: {}", request.username());
            throw new IllegalArgumentException("Invalid username or password");
        }

        String sessionToken = generateSessionToken();
        Instant expiresAt = Instant.now().plus(36500, ChronoUnit.DAYS);

        AdminSession session = AdminSession.builder()
                .adminId(admin.getId())
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();
        adminSessionRepository.save(session);

        admin.setLastLoginAt(Instant.now());
        adminRepository.save(admin);

        log.info("Admin logged in: {}", admin.getId());

        return new AdminLoginResponseDto(
                sessionToken,
                expiresAt,
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                admin.getFullName(),
                admin.getRole()
        );
    }

    @Transactional
    public void logout(String sessionToken) {
        log.debug("Admin logout request");
        adminSessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setStatus(BaseEntity.STATUS_INACTIVE);
            adminSessionRepository.save(session);
            log.info("Admin session marked inactive on logout for admin: {}", session.getAdminId());
        });
    }

    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
