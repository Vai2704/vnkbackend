package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByIdAndStatus(UUID id, Integer status);

    default Optional<ProductCategory> findByIdAndStatusActive(UUID id) {
        return findByIdAndStatus(id, BaseEntity.STATUS_ACTIVE);
    }

    List<ProductCategory> findByStatusOrderByDisplayOrderAsc(Integer status);

    default List<ProductCategory> findAllActive() {
        return findByStatusOrderByDisplayOrderAsc(BaseEntity.STATUS_ACTIVE);
    }

    List<ProductCategory> findByParentIdAndStatus(UUID parentId, Integer status);

    default List<ProductCategory> findByParentIdActive(UUID parentId) {
        return findByParentIdAndStatus(parentId, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
