package com.example.vnkapp.dto.user;

import java.time.Instant;

public record LoginResponseDto(
        String sessionToken,
        Instant expiresAt,
        int expiryHours
) {}
