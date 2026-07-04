package com.example.vnkapp.service;

import com.example.vnkapp.dto.review.ReviewCreateRequestDto;
import com.example.vnkapp.dto.review.ReviewResponseDto;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductReview;
import com.example.vnkapp.enums.product.ReviewStatus;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ProductReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewService.class);

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;

    public ProductReviewService(ProductReviewRepository productReviewRepository,
                                ProductRepository productRepository) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ReviewResponseDto addReview(UUID productId, UUID userId, ReviewCreateRequestDto dto) {
        log.debug("Adding review for product: {}, user: {}", productId, userId);

        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        if (productReviewRepository.existsByProductIdAndUserId(productId, userId)) {
            log.warn("User {} already reviewed product {}", userId, productId);
            throw new IllegalArgumentException("You have already submitted a review for this product");
        }

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .userId(userId)
                .rating(dto.rating())
                .title(dto.title())
                .comment(dto.comment())
                .reviewStatus(ReviewStatus.APPROVED)
                .isVerifiedPurchase(false)
                .build();

        ProductReview savedReview = productReviewRepository.save(review);
        log.info("Review saved: {} for product: {}", savedReview.getId(), productId);

        updateProductRatingStats(product, productId);

        return ReviewResponseDto.fromEntity(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForProduct(UUID productId) {
        log.debug("Fetching reviews for product: {}", productId);
        productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        return productReviewRepository.findApprovedByProductId(productId)
                .stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(UUID productId, UUID reviewId) {
        log.debug("Fetching review: {} for product: {}", reviewId, productId);
        productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        ProductReview review = productReviewRepository.findByIdAndStatusActive(reviewId)
                .orElseThrow(() -> {
                    log.warn("Review not found: {}", reviewId);
                    return new IllegalArgumentException("Review not found");
                });

        if (!review.getProductId().equals(productId)) {
            log.warn("Review {} does not belong to product {}", reviewId, productId);
            throw new IllegalArgumentException("Review not found for this product");
        }

        return ReviewResponseDto.fromEntity(review);
    }

    private void updateProductRatingStats(Product product, UUID productId) {
        long count = productReviewRepository.countApprovedByProductId(productId);
        Double avg = productReviewRepository.avgRatingByProductId(productId);

        product.setReviewCount((int) count);
        product.setAverageRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        productRepository.save(product);
        log.debug("Updated product {} stats: avg={}, count={}", productId, avg, count);
    }
}
