package com.example.vnkapp.dto.family;

import com.example.vnkapp.enums.common.Gender;
import com.example.vnkapp.enums.common.Relationship;

import java.time.LocalDate;
import java.util.List;

public record FamilyMemberUpdateRequestDto(
        String name,
        Relationship relationship,
        Gender gender,
        LocalDate dateOfBirth,
        String phone,
        String email,
        String bloodGroup,
        String profileImageUrl,
        List<String> diseases,
        List<String> medications
) {}
