package com.example.vnkapp.dto.profile;

import com.example.vnkapp.entity.User;
import com.example.vnkapp.enums.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponseDto(
        UUID id,
        String username,
        String email,
        String phone,
        String profileImageUrl,
        Boolean isEmailVerified,
        Boolean isPhoneVerified,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProfileResponseDto fromEntity(User user) {
        return new ProfileResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getIsEmailVerified(),
                user.getIsPhoneVerified(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
