package com.example.vnkapp.dto.category;

import java.util.UUID;

public record CategoryUpdateRequestDto(
        String name,

        String description,

        String imageUrl,

        UUID parentId,

        Integer displayOrder,

        Boolean isFeatured
) {}
