package com.example.vnkapp.dto.family;

import com.example.vnkapp.enums.common.Gender;
import com.example.vnkapp.enums.common.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FamilyMemberCreateRequestDto(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Relationship is required")
        Relationship relationship,

        Gender gender,

        LocalDate dateOfBirth,

        String phone,

        String email,

        String bloodGroup,

        String profileImageUrl,

        String disease,

        String medication
) {}
