package com.example.vnkapp.service;

import com.example.vnkapp.dto.user.ForgotPasswordRequestDto;
import com.example.vnkapp.dto.user.LoginResponseDto;
import com.example.vnkapp.dto.user.ResetPasswordRequestDto;
import com.example.vnkapp.dto.user.UserLoginRequestDto;
import com.example.vnkapp.dto.user.UserRegisterRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.PasswordResetToken;
import com.example.vnkapp.entity.ReferralCode;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.entity.UserSession;
import com.example.vnkapp.repository.PasswordResetTokenRepository;
import com.example.vnkapp.repository.ReferralCodeRepository;
import com.example.vnkapp.repository.UserRepository;
import com.example.vnkapp.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String MEMBER_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MEMBER_ID_LENGTH = 8;
    private static final int MEMBER_ID_MAX_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Optional<EmailService> emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.expiry-minutes:5}")
    private int passwordResetExpiryMinutes;

    public UserService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       ReferralCodeRepository referralCodeRepository,
                       PasswordEncoder passwordEncoder,
                       Optional<EmailService> emailService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.referralCodeRepository = referralCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public LoginResponseDto register(UserRegisterRequestDto registerRequestDto) {
        log.debug("Registering user: {}", registerRequestDto.email());
        if (userRepository.existsByEmail(registerRequestDto.email())) {
            log.warn("Registration failed - email already in use: {}", registerRequestDto.email());
            throw new DataIntegrityViolationException("Email already in use");
        }

        User user = User.builder()
                .username(registerRequestDto.username())
                .email(registerRequestDto.email())
                .password(passwordEncoder.encode(registerRequestDto.password()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        String memberId = generateMemberId();
        ReferralCode referralCode = ReferralCode.builder()
                .userId(savedUser.getId())
                .code(memberId)
                .build();
        referralCodeRepository.save(referralCode);
        log.info("Member ID {} generated for user: {}", memberId, savedUser.getId());

        // Send welcome email asynchronously (if email service is configured)
        emailService.ifPresent(service -> service.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername(), memberId));

        // Auto-login: Create session and return token
        String sessionToken = generateSessionToken();
        Instant expiresAt = Instant.now().plus(36500, ChronoUnit.DAYS);

        UserSession session = UserSession.builder()
                .userId(savedUser.getId())
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);

        return new LoginResponseDto(sessionToken, expiresAt);
    }

    @Transactional
    public LoginResponseDto login(UserLoginRequestDto dto) {
        log.debug("Login attempt for: {}", dto.email());
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", dto.email());
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            log.warn("Login failed - wrong password for: {}", dto.email());
            throw new IllegalArgumentException("Invalid email or password");
        }

        String sessionToken = generateSessionToken();
        Instant expiresAt = Instant.now().plus(36500, ChronoUnit.DAYS);

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);
        log.info("User logged in: {}", user.getId());

        return new LoginResponseDto(sessionToken, expiresAt);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto dto) {
        log.debug("Forgot password for: {}", dto.email());
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> {
                    log.warn("Forgot password - user not found: {}", dto.email());
                    return new IllegalArgumentException("No account found with this email");
                });

        // Invalidate any existing reset tokens for this email
        passwordResetTokenRepository.invalidateAllTokensForEmail(dto.email());

        // Generate 6-digit code
        String code = generateResetCode();
        Instant expiresAt = Instant.now().plus(passwordResetExpiryMinutes, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .email(dto.email())
                .code(code)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset code generated for user: {}", user.getId());

        // Send reset code email asynchronously (if email service is configured)
        emailService.ifPresent(service -> service.sendPasswordResetCode(dto.email(), code));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto dto) {
        log.debug("Reset password for: {}", dto.email());
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailAndCodeAndIsUsedFalse(dto.email(), dto.code())
                .orElseThrow(() -> {
                    log.warn("Reset password - invalid or expired code for: {}", dto.email());
                    return new IllegalArgumentException("Invalid or expired reset code");
                });

        if (!resetToken.isValid()) {
            log.warn("Reset password - code expired for: {}", dto.email());
            throw new IllegalArgumentException("Reset code has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getId());

        // Send confirmation email asynchronously (if email service is configured)
        emailService.ifPresent(service -> service.sendPasswordResetSuccess(user.getEmail(), user.getUsername()));
    }

    @Transactional
    public void logout(String sessionToken) {
        log.debug("Logout request for token");
        userSessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setStatus(BaseEntity.STATUS_INACTIVE);
            userSessionRepository.save(session);
            log.info("Session marked inactive on logout for user: {}", session.getUserId());
        });
    }

    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String generateResetCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private String generateMemberId() {
        for (int attempt = 0; attempt < MEMBER_ID_MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(MEMBER_ID_LENGTH);
            for (int i = 0; i < MEMBER_ID_LENGTH; i++) {
                sb.append(MEMBER_ID_ALPHABET.charAt(secureRandom.nextInt(MEMBER_ID_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (!referralCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique member ID after " + MEMBER_ID_MAX_ATTEMPTS + " attempts");
    }
}
