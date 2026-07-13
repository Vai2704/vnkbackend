package com.example.vnkapp.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequestDto(
        UUID categoryId,

        String name,

        String sku,

        String description,

        String shortDescription,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        BigDecimal compareAtPrice,

        BigDecimal costPrice,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        Integer lowStockThreshold,

        Integer weightGrams,

        String packSize,

        String brand,

        String ingredients,

        String howToUse,

        Boolean isFeatured,

        String metaTitle,

        String metaDescription
) {}
