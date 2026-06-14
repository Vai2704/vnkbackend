package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberCreateRequestDto;
import com.example.vnkapp.dto.family.FamilyMemberResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberUpdateRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.FamilyMemberService;
import jakarta.validation.Valid;
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

    private final FamilyMemberService familyMemberService;

    public FamilyController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @PostMapping
    public ResponseEntity<?> addFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody FamilyMemberCreateRequestDto request) {
        try {
            FamilyMemberResponseDto member = familyMemberService.addFamilyMember(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add family member due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody FamilyMemberUpdateRequestDto request) {
        try {
            FamilyMemberResponseDto member = familyMemberService.updateFamilyMember(currentUser.getId(), id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update family member due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        try {
            familyMemberService.deleteFamilyMember(currentUser.getId(), id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete family member due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFamilyMembers(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        try {
            List<FamilyMemberResponseDto> members = familyMemberService.getAllFamilyMembers(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, members));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch family members due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFamilyMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        try {
            FamilyMemberResponseDto member = familyMemberService.getFamilyMember(currentUser.getId(), id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, member));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch family member due to some issue."));
        }
    }
}
