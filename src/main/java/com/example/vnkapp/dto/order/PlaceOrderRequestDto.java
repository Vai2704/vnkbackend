package com.example.vnkapp.dto.order;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaceOrderRequestDto(
        @NotNull(message = "Address ID is required")
        UUID addressId,

        UUID couponId,

        String notes
) {}
