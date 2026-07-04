package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.review.ReviewCreateRequestDto;
import com.example.vnkapp.dto.review.ReviewResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.ProductReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewController.class);

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @PostMapping
    public ResponseEntity<?> addReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReviewCreateRequestDto request) {
        log.info("Add review for product: {} by user: {}", productId, currentUser.getId());
        try {
            ReviewResponseDto review = productReviewService.addReview(productId, currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, review));
        } catch (IllegalArgumentException ex) {
            log.warn("Add review failed for product {}: {}", productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Add review error for product: {}", productId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add review due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getReviews(@PathVariable UUID productId) {
        log.info("Get reviews for product: {}", productId);
        try {
            List<ReviewResponseDto> reviews = productReviewService.getReviewsForProduct(productId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, reviews));
        } catch (IllegalArgumentException ex) {
            log.warn("Get reviews failed for product {}: {}", productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get reviews error for product: {}", productId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch reviews due to some issue."));
        }
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId) {
        log.info("Get review: {} for product: {}", reviewId, productId);
        try {
            ReviewResponseDto review = productReviewService.getReview(productId, reviewId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, review));
        } catch (IllegalArgumentException ex) {
            log.warn("Get review {} failed for product {}: {}", reviewId, productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get review {} error for product: {}", reviewId, productId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch review due to some issue."));
        }
    }
}
