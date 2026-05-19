package com.example.vnkapp.repository;

import com.example.vnkapp.entity.Address;
import com.example.vnkapp.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndStatusOrderByIsDefaultDescCreatedAtDesc(UUID userId, Integer status);

    default List<Address> findByUserIdActive(UUID userId) {
        return findByUserIdAndStatusOrderByIsDefaultDescCreatedAtDesc(userId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<Address> findByIdAndUserIdAndStatus(UUID id, UUID userId, Integer status);

    default Optional<Address> findByIdAndUserIdActive(UUID id, UUID userId) {
        return findByIdAndUserIdAndStatus(id, userId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<Address> findByUserIdAndIsDefaultAndStatus(UUID userId, Boolean isDefault, Integer status);

    default Optional<Address> findDefaultByUserIdActive(UUID userId) {
        return findByUserIdAndIsDefaultAndStatus(userId, true, BaseEntity.STATUS_ACTIVE);
    }

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.status = 1")
    void clearDefaultForUser(UUID userId);
}
