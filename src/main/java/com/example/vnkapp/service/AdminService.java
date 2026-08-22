package com.example.vnkapp.service;

import com.example.vnkapp.dto.admin.AdminCreateRequestDto;
import com.example.vnkapp.dto.admin.AdminLoginRequestDto;
import com.example.vnkapp.dto.admin.AdminLoginResponseDto;
import com.example.vnkapp.dto.admin.AdminResponseDto;
import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.enums.admin.AdminRole;
import com.example.vnkapp.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminResponseDto getAdminProfile(UUID adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        return toResponseDto(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminResponseDto> listAdmins() {
        return adminRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public AdminResponseDto createAdmin(AdminCreateRequestDto request, UUID createdById) {
        log.debug("Creating admin with username: {}", request.username());

        if (adminRepository.existsByUsername(request.username())) {
            throw new DataIntegrityViolationException("Username already in use");
        }
        if (adminRepository.existsByEmail(request.email())) {
            throw new DataIntegrityViolationException("Email already in use");
        }

        Admin admin = Admin.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(AdminRole.ADMIN)
                .createdById(createdById)
                .build();

        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin created: {} by super admin: {}", savedAdmin.getId(), createdById);
        return toResponseDto(savedAdmin);
    }

    @Transactional
    public AdminResponseDto deactivateAdmin(UUID adminId, UUID requestedById) {
        if (adminId.equals(requestedById)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin accounts cannot be deactivated");
        }

        admin.setStatus(BaseEntity.STATUS_INACTIVE);
        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin deactivated: {} by super admin: {}", adminId, requestedById);
        return toResponseDto(savedAdmin);
    }

    AdminResponseDto toResponseDto(Admin admin) {
        return new AdminResponseDto(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                admin.getFullName(),
                admin.getRole(),
                admin.getCreatedById(),
                admin.getLastLoginAt(),
                admin.getCreatedAt(),
                admin.isActive()
        );
    }
}
