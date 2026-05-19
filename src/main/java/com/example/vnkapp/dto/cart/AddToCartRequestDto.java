package com.example.vnkapp.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequestDto(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
    public AddToCartRequestDto {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
