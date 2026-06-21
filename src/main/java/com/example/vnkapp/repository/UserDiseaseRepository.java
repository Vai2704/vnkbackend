package com.example.vnkapp.repository;

import com.example.vnkapp.entity.UserDisease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserDiseaseRepository extends JpaRepository<UserDisease, UUID> {

    boolean existsByUserIdAndFamilyMemberIdAndDiseaseId(UUID userId, UUID familyMemberId, UUID diseaseId);
}
