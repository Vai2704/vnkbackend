package com.example.vnkapp.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductCreateRequestDto(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "Product name is required")
        String name,

        @NotBlank(message = "SKU is required")
        String sku,

        String description,

        String shortDescription,

        @NotNull(message = "Price is required")
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

        Map<String, String> usps,

        String disclaimer,

        Boolean isFeatured,

        String metaTitle,

        String metaDescription
) {}
