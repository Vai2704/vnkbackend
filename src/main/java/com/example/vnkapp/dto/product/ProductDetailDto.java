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
        String packSize,
        String brand,
        String ingredients,
        String howToUse,
        Boolean isFeatured,
        BigDecimal averageRating,
        Integer reviewCount,
        Boolean isWishlisted,
        UUID wishlistId,
        Boolean isInCart,
        Integer cartQuantity,
        List<ReviewResponseDto> reviews
) {
    public static ProductDetailDto fromEntity(Product product, UUID wishlistId, Integer cartQuantity, List<ReviewResponseDto> reviews) {
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
                product.getPackSize(),
                product.getBrand(),
                product.getIngredients(),
                product.getHowToUse(),
                product.getIsFeatured(),
                product.getAverageRating(),
                product.getReviewCount(),
                wishlistId != null,
                wishlistId,
                cartQuantity != null,
                cartQuantity,
                reviews
        );
    }
}
