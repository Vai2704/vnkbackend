package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    List<Wishlist> findByUserIdAndStatus(UUID userId, Integer status);

    default List<Wishlist> findByUserIdActive(UUID userId) {
        return findByUserIdAndStatus(userId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<Wishlist> findByUserIdAndProductIdAndStatus(UUID userId, UUID productId, Integer status);

    default Optional<Wishlist> findByUserIdAndProductIdActive(UUID userId, UUID productId) {
        return findByUserIdAndProductIdAndStatus(userId, productId, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsByUserIdAndProductIdAndStatus(UUID userId, UUID productId, Integer status);

    default boolean existsByUserIdAndProductIdActive(UUID userId, UUID productId) {
        return existsByUserIdAndProductIdAndStatus(userId, productId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT w.productId FROM Wishlist w WHERE w.userId = :userId AND w.status = :status")
    Set<UUID> findProductIdsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Integer status);

    default Set<UUID> findWishlistedProductIds(UUID userId) {
        return findProductIdsByUserIdAndStatus(userId, BaseEntity.STATUS_ACTIVE);
    }
}
