package com.example.vnkapp.dto;

import java.time.Instant;

public record LoginResponseDto(
        String sessionToken,
        Instant expiresAt,
        int expiryHours
) {}
