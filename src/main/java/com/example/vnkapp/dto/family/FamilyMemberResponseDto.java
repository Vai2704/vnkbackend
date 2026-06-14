package com.example.vnkapp.dto.family;

import com.example.vnkapp.entity.FamilyMember;
import com.example.vnkapp.enums.common.Gender;
import com.example.vnkapp.enums.common.Relationship;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FamilyMemberResponseDto(
        UUID id,
        String name,
        Relationship relationship,
        Gender gender,
        LocalDate dateOfBirth,
        String phone,
        String email,
        String bloodGroup,
        String profileImageUrl,
        String disease,
        String medication,
        Instant createdAt,
        Instant updatedAt
) {
    public static FamilyMemberResponseDto fromEntity(FamilyMember familyMember) {
        return new FamilyMemberResponseDto(
                familyMember.getId(),
                familyMember.getName(),
                familyMember.getRelationship(),
                familyMember.getGender(),
                familyMember.getDateOfBirth(),
                familyMember.getPhone(),
                familyMember.getEmail(),
                familyMember.getBloodGroup(),
                familyMember.getProfileImageUrl(),
                familyMember.getDisease(),
                familyMember.getMedication(),
                familyMember.getCreatedAt(),
                familyMember.getUpdatedAt()
        );
    }
}
