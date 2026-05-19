package com.example.vnkapp.dto.wishlist;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToWishlistRequestDto(
        @NotNull(message = "Product ID is required")
        UUID productId
) {}
