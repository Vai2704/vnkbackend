package com.example.vnkapp.dto.category;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryCreateRequestDto(
        @NotBlank(message = "Category name is required")
        String name,

        String description,

        String imageUrl,

        UUID parentId,

        Integer displayOrder,

        Boolean isFeatured
) {}
