package com.example.vnkapp.dto.product;

import com.example.vnkapp.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryDto(
        UUID id,
        UUID categoryId,
        String name,
        String slug,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String brand,
        Boolean isFeatured,
        BigDecimal averageRating,
        Integer reviewCount,
        Integer stockQuantity
) {
    public static ProductSummaryDto fromEntity(Product product) {
        return new ProductSummaryDto(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getBrand(),
                product.getIsFeatured(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getStockQuantity()
        );
    }
}
