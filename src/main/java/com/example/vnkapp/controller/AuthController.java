package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.ForgotPasswordRequestDto;
import com.example.vnkapp.dto.user.LoginResponseDto;
import com.example.vnkapp.dto.user.ResetPasswordRequestDto;
import com.example.vnkapp.dto.user.UserLoginRequestDto;
import com.example.vnkapp.dto.user.UserRegisterRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequestDto request) {
        log.info("Register request for email: {}", request.email());
        try {
            LoginResponseDto loginResponse = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, loginResponse));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Register failed - email already in use: {}", request.email());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, "Email already in use."));
        } catch (Exception ex) {
            log.error("Register error for email: {}", request.email(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't create user due to some issue."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequestDto request) {
        log.info("Login request for email: {}", request.email());
        try {
            LoginResponseDto loginResponse = userService.login(request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, loginResponse));
        } catch (IllegalArgumentException ex) {
            log.warn("Login failed - invalid credentials for email: {}", request.email());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, "Invalid email or password."));
        } catch (Exception ex) {
            log.error("Login error for email: {}", request.email(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't login user due to some issue."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<UserResponseDto> logout(HttpServletRequest request) {
        log.info("Logout request received");
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, "Missing or invalid Authorization header."));
        }
        String token = header.substring(7).trim();
        try {
            userService.logout(token);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (Exception ex) {
            log.error("Logout error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Logout failed."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<UserResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        log.info("Forgot password request for email: {}", request.email());
        try {
            userService.forgotPassword(request);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Forgot password failed for email: {} - {}", request.email(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Forgot password error for email: {}", request.email(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't process forgot password request."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<UserResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        log.info("Reset password request for email: {}", request.email());
        try {
            userService.resetPassword(request);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Reset password failed for email: {} - {}", request.email(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Reset password error for email: {}", request.email(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't reset password due to some issue."));
        }
    }
}
