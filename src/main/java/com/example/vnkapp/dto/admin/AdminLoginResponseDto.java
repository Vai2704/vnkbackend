package com.example.vnkapp.dto.admin;

import com.example.vnkapp.enums.admin.AdminRole;

import java.time.Instant;
import java.util.UUID;

public record AdminLoginResponseDto(
        String sessionToken,
        Instant expiresAt,
        UUID id,
        String username,
        String email,
        String fullName,
        AdminRole role
) {}
