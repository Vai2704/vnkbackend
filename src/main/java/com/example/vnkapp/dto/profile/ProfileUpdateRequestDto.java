package com.example.vnkapp.dto.profile;

public record ProfileUpdateRequestDto(
        String username,
        String phone,
        String profileImageUrl
) {}
