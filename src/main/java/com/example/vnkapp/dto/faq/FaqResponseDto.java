package com.example.vnkapp.dto.faq;

import com.example.vnkapp.entity.Faq;

import java.time.Instant;
import java.util.UUID;

public record FaqResponseDto(
        UUID id,
        String question,
        String answer,
        Integer displayOrder,
        Integer status,
        Instant createdAt,
        Instant updatedAt
) {
    public static FaqResponseDto fromEntity(Faq faq) {
        return new FaqResponseDto(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getDisplayOrder(),
                faq.getStatus(),
                faq.getCreatedAt(),
                faq.getUpdatedAt()
        );
    }
}
