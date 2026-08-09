package com.example.vnkapp.dto.dashboard;

import java.time.Instant;
import java.util.UUID;

public record DashboardReviewDto(
        UUID id,
        UUID productId,
        String productName,
        String productThumbnail,
        UUID userId,
        String userName,
        Integer rating,
        String title,
        String comment,
        Boolean isVerifiedPurchase,
        Instant createdAt
) {}
