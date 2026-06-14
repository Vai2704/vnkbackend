package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    List<FamilyMember> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Integer status);

    default List<FamilyMember> findByUserIdActive(UUID userId) {
        return findByUserIdAndStatusOrderByCreatedAtDesc(userId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<FamilyMember> findByIdAndUserIdAndStatus(UUID id, UUID userId, Integer status);

    default Optional<FamilyMember> findByIdAndUserIdActive(UUID id, UUID userId) {
        return findByIdAndUserIdAndStatus(id, userId, BaseEntity.STATUS_ACTIVE);
    }
}
