package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiseaseRepository extends JpaRepository<Disease, UUID> {

    Optional<Disease> findByNameIgnoreCaseAndStatus(String name, Integer status);

    default Optional<Disease> findActiveByName(String name) {
        return findByNameIgnoreCaseAndStatus(name, BaseEntity.STATUS_ACTIVE);
    }
}
