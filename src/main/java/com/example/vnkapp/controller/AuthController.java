package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.ForgotPasswordRequestDto;
import com.example.vnkapp.dto.user.LoginResponseDto;
import com.example.vnkapp.dto.user.ResetPasswordRequestDto;
import com.example.vnkapp.dto.user.UserLoginRequestDto;
import com.example.vnkapp.dto.user.UserRegisterRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequestDto request) {
        try {
            LoginResponseDto loginResponse = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, loginResponse));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, "Email already in use."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't create user due to some issue."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequestDto request) {
        try {
            LoginResponseDto loginResponse = userService.login(request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, loginResponse));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, "Invalid email or password."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't login user due to some issue."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<UserResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        try {
            userService.forgotPassword(request);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't process forgot password request."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<UserResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        try {
            userService.resetPassword(request);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't reset password due to some issue."));
        }
    }
}
