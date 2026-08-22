package com.example.vnkapp.controller.admin;

import com.example.vnkapp.dto.admin.AdminLoginRequestDto;
import com.example.vnkapp.dto.admin.AdminLoginResponseDto;
import com.example.vnkapp.dto.admin.AdminMessageResponseDto;
import com.example.vnkapp.dto.admin.AdminResponseDto;
import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.security.AuthenticatedAdmin;
import com.example.vnkapp.service.AdminAuthService;
import com.example.vnkapp.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;

    public AdminAuthController(AdminAuthService adminAuthService, AdminService adminService) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequestDto request) {
        log.info("Admin login request for username: {}", request.username());
        try {
            AdminLoginResponseDto loginResponse = adminAuthService.login(request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, loginResponse));
        } catch (IllegalArgumentException ex) {
            log.warn("Admin login failed for username: {} - {}", request.username(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminMessageResponseDto(null, "Invalid username or password."));
        } catch (Exception ex) {
            log.error("Admin login error for username: {}", request.username(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Can't login admin due to some issue."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AdminMessageResponseDto> logout(HttpServletRequest request) {
        log.info("Admin logout request received");
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminMessageResponseDto(null, "Missing or invalid Authorization header."));
        }

        String token = header.substring(7).trim();
        try {
            adminAuthService.logout(token);
            return ResponseEntity.ok(new AdminMessageResponseDto("Ok", null));
        } catch (Exception ex) {
            log.error("Admin logout error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Logout failed."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@AuthenticationPrincipal AuthenticatedAdmin currentAdmin) {
        if (currentAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminMessageResponseDto(null, "Authentication required."));
        }

        try {
            AdminResponseDto profile = adminService.getAdminProfile(currentAdmin.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, profile));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminMessageResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to fetch admin profile for id: {}", currentAdmin.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Can't fetch admin profile."));
        }
    }
}
