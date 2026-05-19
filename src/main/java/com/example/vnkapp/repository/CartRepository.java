package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, Integer status);

    default Optional<Cart> findByUserIdActive(UUID userId) {
        return findByUserIdAndStatus(userId, BaseEntity.STATUS_ACTIVE);
    }
}
