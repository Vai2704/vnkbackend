package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Product> findByCategoryIdAndStatus(UUID categoryId, Integer status, Pageable pageable);

    default List<Product> findTopByCategoryActive(UUID categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        return findByCategoryIdAndStatus(categoryId, BaseEntity.STATUS_ACTIVE, pageable);
    }

    List<Product> findByIsFeaturedAndStatus(Boolean isFeatured, Integer status, Pageable pageable);

    default List<Product> findFeaturedActive(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "reviewCount")));
        return findByIsFeaturedAndStatus(true, BaseEntity.STATUS_ACTIVE, pageable);
    }

    default List<Product> findLatestActive(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return findByStatus(BaseEntity.STATUS_ACTIVE, pageable).getContent();
    }

    @Query("SELECT p FROM Product p WHERE p.status = 1 AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchActiveProducts(@Param("query") String query, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySkuAndIdNot(String sku, UUID id);
}
