package com.example.vnkapp.dto.admin;

import com.example.vnkapp.enums.admin.AdminRole;

import java.time.Instant;
import java.util.UUID;

public record AdminResponseDto(
        UUID id,
        String username,
        String email,
        String fullName,
        AdminRole role,
        UUID createdById,
        Instant lastLoginAt,
        Instant createdAt,
        boolean active
) {}
