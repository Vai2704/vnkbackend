package com.example.vnkapp.repository;

import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.enums.admin.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(AdminRole role);

    List<Admin> findAllByOrderByCreatedAtDesc();
}
