package com.example.vnkapp.dto.faq;

public record FaqUpdateRequestDto(
        String question,

        String answer,

        Integer displayOrder
) {}
