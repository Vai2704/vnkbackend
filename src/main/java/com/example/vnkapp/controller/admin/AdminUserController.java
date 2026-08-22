package com.example.vnkapp.controller.admin;

import com.example.vnkapp.dto.admin.AdminCreateRequestDto;
import com.example.vnkapp.dto.admin.AdminMessageResponseDto;
import com.example.vnkapp.dto.admin.AdminResponseDto;
import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.security.AuthenticatedAdmin;
import com.example.vnkapp.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> listAdmins(@AuthenticationPrincipal AuthenticatedAdmin currentAdmin) {
        log.info("List admins request by super admin: {}", currentAdmin.getId());
        try {
            List<AdminResponseDto> admins = adminService.listAdmins();
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, admins));
        } catch (Exception ex) {
            log.error("Failed to list admins", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Can't fetch admins."));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(
            @AuthenticationPrincipal AuthenticatedAdmin currentAdmin,
            @Valid @RequestBody AdminCreateRequestDto request) {
        log.info("Create admin request by super admin: {}", currentAdmin.getId());
        try {
            AdminResponseDto admin = adminService.createAdmin(request, currentAdmin.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, admin));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Create admin failed - duplicate value: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminMessageResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Create admin error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Can't create admin due to some issue."));
        }
    }

    @DeleteMapping("/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deactivateAdmin(
            @AuthenticationPrincipal AuthenticatedAdmin currentAdmin,
            @PathVariable UUID adminId) {
        log.info("Deactivate admin request for id: {} by super admin: {}", adminId, currentAdmin.getId());
        try {
            AdminResponseDto admin = adminService.deactivateAdmin(adminId, currentAdmin.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, admin));
        } catch (IllegalArgumentException ex) {
            log.warn("Deactivate admin failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminMessageResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Deactivate admin error for id: {}", adminId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminMessageResponseDto(null, "Can't deactivate admin."));
        }
    }
}
