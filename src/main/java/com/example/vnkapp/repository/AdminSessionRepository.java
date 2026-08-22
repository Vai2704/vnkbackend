package com.example.vnkapp.repository;

import com.example.vnkapp.entity.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminSessionRepository extends JpaRepository<AdminSession, UUID> {

    Optional<AdminSession> findBySessionToken(String sessionToken);

    void deleteByAdminId(UUID adminId);
}
