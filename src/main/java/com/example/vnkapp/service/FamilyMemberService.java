package com.example.vnkapp.service;

import com.example.vnkapp.dto.family.FamilyMemberCreateRequestDto;
import com.example.vnkapp.dto.family.FamilyMemberResponseDto;
import com.example.vnkapp.dto.family.FamilyMemberUpdateRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Disease;
import com.example.vnkapp.entity.FamilyMember;
import com.example.vnkapp.entity.Medication;
import com.example.vnkapp.entity.UserDisease;
import com.example.vnkapp.entity.UserMedication;
import com.example.vnkapp.repository.DiseaseRepository;
import com.example.vnkapp.repository.FamilyMemberRepository;
import com.example.vnkapp.repository.MedicationRepository;
import com.example.vnkapp.repository.UserDiseaseRepository;
import com.example.vnkapp.repository.UserMedicationRepository;
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
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final UserDiseaseRepository userDiseaseRepository;
    private final UserMedicationRepository userMedicationRepository;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                               DiseaseRepository diseaseRepository,
                               MedicationRepository medicationRepository,
                               UserDiseaseRepository userDiseaseRepository,
                               UserMedicationRepository userMedicationRepository) {
        this.familyMemberRepository = familyMemberRepository;
        this.diseaseRepository = diseaseRepository;
        this.medicationRepository = medicationRepository;
        this.userDiseaseRepository = userDiseaseRepository;
        this.userMedicationRepository = userMedicationRepository;
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
                .build();

        FamilyMember savedMember = familyMemberRepository.save(familyMember);
        log.info("Family member created: {} for user: {}", savedMember.getId(), userId);

        linkDiseases(userId, savedMember.getId(), dto.diseases());
        linkMedications(userId, savedMember.getId(), dto.medications());

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

        FamilyMember updatedMember = familyMemberRepository.save(familyMember);

        linkDiseases(userId, memberId, dto.diseases());
        linkMedications(userId, memberId, dto.medications());

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

    private void linkDiseases(UUID userId, UUID familyMemberId, List<String> diseaseNames) {
        if (diseaseNames == null || diseaseNames.isEmpty()) return;

        for (String rawName : diseaseNames) {
            if (rawName == null || rawName.isBlank()) continue;
            String name = rawName.trim();

            Disease disease = diseaseRepository.findActiveByName(name)
                    .orElseGet(() -> {
                        log.debug("Disease not found in master, creating new entry: {}", name);
                        return diseaseRepository.save(Disease.builder().name(name).build());
                    });

            // skip if this family member already has this disease linked
            if (userDiseaseRepository.existsByUserIdAndFamilyMemberIdAndDiseaseId(userId, familyMemberId, disease.getId())) {
                log.debug("Disease {} already linked to family member {}, skipping", name, familyMemberId);
                continue;
            }

            userDiseaseRepository.save(UserDisease.builder()
                    .userId(userId)
                    .familyMemberId(familyMemberId)
                    .diseaseId(disease.getId())
                    .build());

            log.debug("Linked disease '{}' to family member {}", name, familyMemberId);
        }
    }

    private void linkMedications(UUID userId, UUID familyMemberId, List<String> medicationNames) {
        if (medicationNames == null || medicationNames.isEmpty()) return;

        for (String rawName : medicationNames) {
            if (rawName == null || rawName.isBlank()) continue;
            String name = rawName.trim();

            Medication medication = medicationRepository.findActiveByName(name)
                    .orElseGet(() -> {
                        log.debug("Medication not found in master, creating new entry: {}", name);
                        return medicationRepository.save(Medication.builder().name(name).build());
                    });

            userMedicationRepository.save(UserMedication.builder()
                    .userId(userId)
                    .familyMemberId(familyMemberId)
                    .medicationId(medication.getId())
                    .build());

            log.debug("Linked medication '{}' to family member {}", name, familyMemberId);
        }
    }
}
