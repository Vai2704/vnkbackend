package com.example.vnkapp.repository;

import com.example.vnkapp.entity.UserMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserMedicationRepository extends JpaRepository<UserMedication, UUID> {
}
