package com.example.vnkapp.dto.family;

import com.example.vnkapp.enums.common.Gender;
import com.example.vnkapp.enums.common.Relationship;

import java.time.LocalDate;

public record FamilyMemberUpdateRequestDto(
        String name,
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
