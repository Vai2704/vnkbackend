package com.example.vnkapp.dto.review;

import com.example.vnkapp.entity.ProductReview;
import com.example.vnkapp.enums.product.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponseDto(
        UUID id,
        UUID productId,
        UUID userId,
        Integer rating,
        String title,
        String comment,
        ReviewStatus reviewStatus,
        Boolean isVerifiedPurchase,
        Integer helpfulCount,
        Instant createdAt
) {
    public static ReviewResponseDto fromEntity(ProductReview review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getReviewStatus(),
                review.getIsVerifiedPurchase(),
                review.getHelpfulCount(),
                review.getCreatedAt()
        );
    }
}
