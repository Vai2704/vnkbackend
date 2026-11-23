package com.example.vnkapp.dto.user;

import java.time.Instant;

public record RegisterResponseDto(
        String sessionToken,
        Instant expiresAt,
        String memberId,
        String welcomeCouponCode
) {}
