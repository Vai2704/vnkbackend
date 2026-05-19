package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductIdAndIsPrimaryAndStatus(UUID productId, Boolean isPrimary, Integer status);

    default Optional<ProductImage> findPrimaryByProductId(UUID productId) {
        return findByProductIdAndIsPrimaryAndStatus(productId, true, BaseEntity.STATUS_ACTIVE);
    }

    List<ProductImage> findByProductIdInAndIsPrimaryAndStatus(List<UUID> productIds, Boolean isPrimary, Integer status);

    default List<ProductImage> findPrimaryByProductIds(List<UUID> productIds) {
        return findByProductIdInAndIsPrimaryAndStatus(productIds, true, BaseEntity.STATUS_ACTIVE);
    }

    List<ProductImage> findByProductIdAndStatusOrderByDisplayOrderAsc(UUID productId, Integer status);

    default List<ProductImage> findByProductIdActive(UUID productId) {
        return findByProductIdAndStatusOrderByDisplayOrderAsc(productId, BaseEntity.STATUS_ACTIVE);
    }
}
