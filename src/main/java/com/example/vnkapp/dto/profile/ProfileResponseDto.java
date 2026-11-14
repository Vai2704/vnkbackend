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
        String memberId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProfileResponseDto fromEntity(User user, String memberId) {
        return new ProfileResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getIsEmailVerified(),
                user.getIsPhoneVerified(),
                user.getRole(),
                memberId,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
