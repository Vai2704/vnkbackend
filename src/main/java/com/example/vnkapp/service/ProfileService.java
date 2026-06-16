package com.example.vnkapp.service;

import com.example.vnkapp.dto.profile.ProfileResponseDto;
import com.example.vnkapp.dto.profile.ProfileUpdateRequestDto;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(UUID userId) {
        log.debug("Fetching profile for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        return ProfileResponseDto.fromEntity(user);
    }

    @Transactional
    public ProfileResponseDto updateProfile(UUID userId, ProfileUpdateRequestDto dto) {
        log.debug("Updating profile for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

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
        log.info("Profile updated for user: {}", userId);
        return ProfileResponseDto.fromEntity(updatedUser);
    }
}
