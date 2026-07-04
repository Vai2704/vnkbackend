package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.ProductReview;
import com.example.vnkapp.enums.product.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    List<ProductReview> findByProductIdAndStatusAndReviewStatusOrderByCreatedAtDesc(
            UUID productId, Integer status, ReviewStatus reviewStatus);

    default List<ProductReview> findApprovedByProductId(UUID productId) {
        return findByProductIdAndStatusAndReviewStatusOrderByCreatedAtDesc(
                productId, BaseEntity.STATUS_ACTIVE, ReviewStatus.APPROVED);
    }

    Optional<ProductReview> findByIdAndStatus(UUID id, Integer status);

    default Optional<ProductReview> findByIdAndStatusActive(UUID id) {
        return findByIdAndStatus(id, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsByProductIdAndUserIdAndStatus(UUID productId, UUID userId, Integer status);

    default boolean existsByProductIdAndUserId(UUID productId, UUID userId) {
        return existsByProductIdAndUserIdAndStatus(productId, userId, BaseEntity.STATUS_ACTIVE);
    }

    long countByProductIdAndStatusAndReviewStatus(UUID productId, Integer status, ReviewStatus reviewStatus);

    default long countApprovedByProductId(UUID productId) {
        return countByProductIdAndStatusAndReviewStatus(productId, BaseEntity.STATUS_ACTIVE, ReviewStatus.APPROVED);
    }

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId AND r.status = :status AND r.reviewStatus = :reviewStatus")
    Double avgRatingByProductIdAndStatusAndReviewStatus(
            @Param("productId") UUID productId,
            @Param("status") Integer status,
            @Param("reviewStatus") ReviewStatus reviewStatus);

    default Double avgRatingByProductId(UUID productId) {
        return avgRatingByProductIdAndStatusAndReviewStatus(productId, BaseEntity.STATUS_ACTIVE, ReviewStatus.APPROVED);
    }
}
