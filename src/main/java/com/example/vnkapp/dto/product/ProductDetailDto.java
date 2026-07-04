package com.example.vnkapp.dto.product;

import com.example.vnkapp.dto.review.ReviewResponseDto;
import com.example.vnkapp.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailDto(
        UUID id,
        UUID categoryId,
        String name,
        String slug,
        String sku,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        Integer stockQuantity,
        Integer weightGrams,
        String brand,
        String ingredients,
        String howToUse,
        Boolean isFeatured,
        BigDecimal averageRating,
        Integer reviewCount,
        List<ReviewResponseDto> reviews
) {
    public static ProductDetailDto fromEntity(Product product, List<ReviewResponseDto> reviews) {
        return new ProductDetailDto(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getStockQuantity(),
                product.getWeightGrams(),
                product.getBrand(),
                product.getIngredients(),
                product.getHowToUse(),
                product.getIsFeatured(),
                product.getAverageRating(),
                product.getReviewCount(),
                reviews
        );
    }
}
