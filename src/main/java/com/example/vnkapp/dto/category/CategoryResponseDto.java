package com.example.vnkapp.dto.category;

import com.example.vnkapp.entity.ProductCategory;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponseDto(
        UUID id,
        String name,
        String slug,
        String description,
        String imageUrl,
        UUID parentId,
        Integer displayOrder,
        Boolean isFeatured,
        Integer status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponseDto fromEntity(ProductCategory category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParentId(),
                category.getDisplayOrder(),
                category.getIsFeatured(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
