package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartIdAndStatus(UUID cartId, Integer status);

    default List<CartItem> findByCartIdActive(UUID cartId) {
        return findByCartIdAndStatus(cartId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<CartItem> findByCartIdAndProductIdAndStatus(UUID cartId, UUID productId, Integer status);

    default Optional<CartItem> findByCartIdAndProductIdActive(UUID cartId, UUID productId) {
        return findByCartIdAndProductIdAndStatus(cartId, productId, BaseEntity.STATUS_ACTIVE);
    }
}
