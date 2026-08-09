package com.example.vnkapp.dto.faq;

import jakarta.validation.constraints.NotBlank;

public record FaqCreateRequestDto(
        @NotBlank(message = "Question is required")
        String question,

        @NotBlank(message = "Answer is required")
        String answer,

        Integer displayOrder
) {}
