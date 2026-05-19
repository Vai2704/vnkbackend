package com.example.vnkapp.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDto(
        UUID cartId,
        List<CartItemResponseDto> items,
        Integer totalItems,
        BigDecimal totalAmount
) {}
