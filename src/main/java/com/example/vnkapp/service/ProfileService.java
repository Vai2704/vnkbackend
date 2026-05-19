package com.example.vnkapp.service;

import com.example.vnkapp.dto.profile.ProfileResponseDto;
import com.example.vnkapp.dto.profile.ProfileUpdateRequestDto;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ProfileResponseDto.fromEntity(user);
    }

    @Transactional
    public ProfileResponseDto updateProfile(UUID userId, ProfileUpdateRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.username() != null && !dto.username().isBlank()) {
            user.setUsername(dto.username());
        }

        if (dto.phone() != null) {
            user.setPhone(dto.phone().isBlank() ? null : dto.phone());
        }

        if (dto.profileImageUrl() != null) {
            user.setProfileImageUrl(dto.profileImageUrl().isBlank() ? null : dto.profileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        return ProfileResponseDto.fromEntity(updatedUser);
    }
}
