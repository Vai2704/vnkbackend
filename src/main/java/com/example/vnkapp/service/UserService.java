package com.example.vnkapp.service;

import com.example.vnkapp.dto.ForgotPasswordRequestDto;
import com.example.vnkapp.dto.LoginResponseDto;
import com.example.vnkapp.dto.ResetPasswordRequestDto;
import com.example.vnkapp.dto.UserLoginRequestDto;
import com.example.vnkapp.dto.UserRegisterRequestDto;
import com.example.vnkapp.entity.PasswordResetToken;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.entity.UserSession;
import com.example.vnkapp.repository.PasswordResetTokenRepository;
import com.example.vnkapp.repository.UserRepository;
import com.example.vnkapp.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.session.expiry-hours:24}")
    private int sessionExpiryHours;

    @Value("${app.password-reset.expiry-minutes:15}")
    private int passwordResetExpiryMinutes;

    public UserService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void register(UserRegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DataIntegrityViolationException("Email already in use");
        }

        User user = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .build();

        userRepository.save(user);

        // Send welcome email asynchronously
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
    }

    @Transactional
    public LoginResponseDto login(UserLoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String sessionToken = generateSessionToken();
        Instant expiresAt = Instant.now().plus(sessionExpiryHours, ChronoUnit.HOURS);

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);

        return new LoginResponseDto(sessionToken, expiresAt, sessionExpiryHours);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

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

        // Send reset code email asynchronously
        emailService.sendPasswordResetCode(dto.email(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailAndCodeAndIsUsedFalse(dto.email(), dto.code())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code"));

        if (!resetToken.isValid()) {
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

        // Send confirmation email asynchronously
        emailService.sendPasswordResetSuccess(user.getEmail(), user.getUsername());
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
}
