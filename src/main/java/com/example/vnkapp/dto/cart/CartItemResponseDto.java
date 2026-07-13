package com.example.vnkapp.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponseDto(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        String packSize,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal totalPrice
) {}
