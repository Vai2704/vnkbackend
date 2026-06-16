package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberCreateRequestDto;
import com.example.vnkapp.dto.family.FamilyMemberResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberUpdateRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.FamilyMemberService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private static final Logger log = LoggerFactory.getLogger(FamilyController.class);

    private final FamilyMemberService familyMemberService;

    public FamilyController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @PostMapping
    public ResponseEntity<?> addFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody FamilyMemberCreateRequestDto request) {
        log.info("Add family member for user: {}, name: {}", currentUser.getId(), request.name());
        try {
            FamilyMemberResponseDto member = familyMemberService.addFamilyMember(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            log.warn("Add family member failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Add family member error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add family member due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody FamilyMemberUpdateRequestDto request) {
        log.info("Update family member {} for user: {}", id, currentUser.getId());
        try {
            FamilyMemberResponseDto member = familyMemberService.updateFamilyMember(currentUser.getId(), id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            log.warn("Update family member {} failed for user: {} - {}", id, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Update family member {} error for user: {}", id, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update family member due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        log.info("Delete family member {} for user: {}", id, currentUser.getId());
        try {
            familyMemberService.deleteFamilyMember(currentUser.getId(), id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Delete family member {} failed for user: {} - {}", id, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Delete family member {} error for user: {}", id, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete family member due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFamilyMembers(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get all family members for user: {}", currentUser.getId());
        try {
            List<FamilyMemberResponseDto> members = familyMemberService.getAllFamilyMembers(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, members));
        } catch (Exception ex) {
            log.error("Get all family members error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch family members due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        log.info("Get family member {} for user: {}", id, currentUser.getId());
        try {
            FamilyMemberResponseDto member = familyMemberService.getFamilyMember(currentUser.getId(), id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            log.warn("Family member {} not found for user: {}", id, currentUser.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get family member {} error for user: {}", id, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch family member due to some issue."));
        }
    }
}
