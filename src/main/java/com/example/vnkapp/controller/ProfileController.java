package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.profile.ProfileResponseDto;
import com.example.vnkapp.dto.profile.ProfileUpdateRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get profile for user: {}", currentUser.getId());
        try {
            ProfileResponseDto profile = profileService.getProfile(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, profile));
        } catch (IllegalArgumentException ex) {
            log.warn("Profile not found for user: {}", currentUser.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get profile error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch profile due to some issue."));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        log.info("Update profile for user: {}", currentUser.getId());
        try {
            ProfileResponseDto profile = profileService.updateProfile(currentUser.getId(), request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, profile));
        } catch (IllegalArgumentException ex) {
            log.warn("Update profile failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Update profile error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update profile due to some issue."));
        }
    }
}
