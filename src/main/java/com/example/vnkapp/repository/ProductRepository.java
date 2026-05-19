package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndStatus(UUID id, Integer status);

    default Optional<Product> findByIdAndStatusActive(UUID id) {
        return findByIdAndStatus(id, BaseEntity.STATUS_ACTIVE);
    }

    List<Product> findByStatusOrderByCreatedAtDesc(Integer status);

    default List<Product> findAllActive() {
        return findByStatusOrderByCreatedAtDesc(BaseEntity.STATUS_ACTIVE);
    }

    Page<Product> findByStatus(Integer status, Pageable pageable);

    default Page<Product> findAllActivePaginated(Pageable pageable) {
        return findByStatus(BaseEntity.STATUS_ACTIVE, pageable);
    }

    List<Product> findByCategoryIdAndStatus(UUID categoryId, Integer status);

    default List<Product> findByCategoryIdActive(UUID categoryId) {
        return findByCategoryIdAndStatus(categoryId, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySkuAndIdNot(String sku, UUID id);
}
