package com.example.vnkapp.dto.product;

import com.example.vnkapp.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponseDto(
        UUID id,
        UUID categoryId,
        String name,
        String slug,
        String sku,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        BigDecimal costPrice,
        Integer stockQuantity,
        Integer lowStockThreshold,
        Integer weightGrams,
        String brand,
        String ingredients,
        String howToUse,
        Integer status,
        Boolean isFeatured,
        BigDecimal averageRating,
        Integer reviewCount,
        String metaTitle,
        String metaDescription,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponseDto fromEntity(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getCostPrice(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.getWeightGrams(),
                product.getBrand(),
                product.getIngredients(),
                product.getHowToUse(),
                product.getStatus(),
                product.getIsFeatured(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getMetaTitle(),
                product.getMetaDescription(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
