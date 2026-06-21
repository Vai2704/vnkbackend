package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    Optional<Medication> findByNameIgnoreCaseAndStatus(String name, Integer status);

    default Optional<Medication> findActiveByName(String name) {
        return findByNameIgnoreCaseAndStatus(name, BaseEntity.STATUS_ACTIVE);
    }
}
