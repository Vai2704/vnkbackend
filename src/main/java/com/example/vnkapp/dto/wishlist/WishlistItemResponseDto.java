package com.example.vnkapp.dto.wishlist;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WishlistItemResponseDto(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        BigDecimal price,
        Boolean inStock,
        Instant addedAt
) {}
