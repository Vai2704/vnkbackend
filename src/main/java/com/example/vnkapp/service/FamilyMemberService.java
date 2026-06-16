package com.example.vnkapp.service;

import com.example.vnkapp.dto.family.FamilyMemberCreateRequestDto;
import com.example.vnkapp.dto.family.FamilyMemberResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberUpdateRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.FamilyMember;
import com.example.vnkapp.repository.FamilyMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FamilyMemberService {

    private static final Logger log = LoggerFactory.getLogger(FamilyMemberService.class);

    private final FamilyMemberRepository familyMemberRepository;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository) {
        this.familyMemberRepository = familyMemberRepository;
    }

    @Transactional
    public FamilyMemberResponseDto addFamilyMember(UUID userId, FamilyMemberCreateRequestDto dto) {
        log.debug("Adding family member for user: {}, name: {}", userId, dto.name());
        FamilyMember familyMember = FamilyMember.builder()
                .userId(userId)
                .name(dto.name())
                .relationship(dto.relationship())
                .gender(dto.gender())
                .dateOfBirth(dto.dateOfBirth())
                .phone(dto.phone())
                .email(dto.email())
                .bloodGroup(dto.bloodGroup())
                .profileImageUrl(dto.profileImageUrl())
                .disease(dto.disease())
                .medication(dto.medication())
                .build();

        FamilyMember savedMember = familyMemberRepository.save(familyMember);
        log.info("Family member added: {} for user: {}", savedMember.getId(), userId);
        return FamilyMemberResponseDto.fromEntity(savedMember);
    }

    @Transactional
    public FamilyMemberResponseDto updateFamilyMember(UUID userId, UUID memberId, FamilyMemberUpdateRequestDto dto) {
        log.debug("Updating family member {} for user: {}", memberId, userId);
        FamilyMember familyMember = familyMemberRepository.findByIdAndUserIdActive(memberId, userId)
                .orElseThrow(() -> {
                    log.warn("Family member {} not found for user: {}", memberId, userId);
                    return new IllegalArgumentException("Family member not found");
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            familyMember.setName(dto.name());
        }

        if (dto.relationship() != null) {
            familyMember.setRelationship(dto.relationship());
        }

        if (dto.gender() != null) {
            familyMember.setGender(dto.gender());
        }

        if (dto.dateOfBirth() != null) {
            familyMember.setDateOfBirth(dto.dateOfBirth());
        }

        if (dto.phone() != null) {
            familyMember.setPhone(dto.phone().isBlank() ? null : dto.phone());
        }

        if (dto.email() != null) {
            familyMember.setEmail(dto.email().isBlank() ? null : dto.email());
        }

        if (dto.bloodGroup() != null) {
            familyMember.setBloodGroup(dto.bloodGroup().isBlank() ? null : dto.bloodGroup());
        }

        if (dto.profileImageUrl() != null) {
            familyMember.setProfileImageUrl(dto.profileImageUrl().isBlank() ? null : dto.profileImageUrl());
        }

        if (dto.disease() != null) {
            familyMember.setDisease(dto.disease().isBlank() ? null : dto.disease());
        }

        if (dto.medication() != null) {
            familyMember.setMedication(dto.medication().isBlank() ? null : dto.medication());
        }

        FamilyMember updatedMember = familyMemberRepository.save(familyMember);
        log.info("Family member updated: {}", memberId);
        return FamilyMemberResponseDto.fromEntity(updatedMember);
    }

    @Transactional
    public void deleteFamilyMember(UUID userId, UUID memberId) {
        log.debug("Deleting family member {} for user: {}", memberId, userId);
        FamilyMember familyMember = familyMemberRepository.findByIdAndUserIdActive(memberId, userId)
                .orElseThrow(() -> {
                    log.warn("Family member {} not found for user: {}", memberId, userId);
                    return new IllegalArgumentException("Family member not found");
                });

        // Soft delete
        familyMember.setStatus(BaseEntity.STATUS_INACTIVE);
        familyMemberRepository.save(familyMember);
        log.info("Family member deleted: {}", memberId);
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponseDto> getAllFamilyMembers(UUID userId) {
        log.debug("Fetching all family members for user: {}", userId);
        return familyMemberRepository.findByUserIdActive(userId)
                .stream()
                .map(FamilyMemberResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyMemberResponseDto getFamilyMember(UUID userId, UUID memberId) {
        log.debug("Fetching family member {} for user: {}", memberId, userId);
        FamilyMember familyMember = familyMemberRepository.findByIdAndUserIdActive(memberId, userId)
                .orElseThrow(() -> {
                    log.warn("Family member {} not found for user: {}", memberId, userId);
                    return new IllegalArgumentException("Family member not found");
                });

        return FamilyMemberResponseDto.fromEntity(familyMember);
    }
}
