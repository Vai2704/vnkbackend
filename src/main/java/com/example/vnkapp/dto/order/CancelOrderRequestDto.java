package com.example.vnkapp.dto.order;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequestDto(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {}
