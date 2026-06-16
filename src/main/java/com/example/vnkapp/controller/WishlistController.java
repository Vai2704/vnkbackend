package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.dto.wishlist.AddToWishlistRequestDto;
import com.example.vnkapp.dto.wishlist.WishlistResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.WishlistService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private static final Logger log = LoggerFactory.getLogger(WishlistController.class);

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToWishlist(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AddToWishlistRequestDto request) {
        log.info("Add to wishlist for user: {}, productId: {}", currentUser.getId(), request.productId());
        try {
            WishlistResponseDto wishlist = wishlistService.addToWishlist(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, wishlist));
        } catch (IllegalArgumentException ex) {
            log.warn("Add to wishlist failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Add to wishlist error for user: {}, productId: {}", currentUser.getId(), request.productId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add product to wishlist due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getWishlist(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get wishlist for user: {}", currentUser.getId());
        try {
            WishlistResponseDto wishlist = wishlistService.getWishlist(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, wishlist));
        } catch (Exception ex) {
            log.error("Get wishlist error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch wishlist due to some issue."));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeFromWishlist(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID itemId) {
        log.info("Remove wishlist item {} for user: {}", itemId, currentUser.getId());
        try {
            WishlistResponseDto wishlist = wishlistService.removeFromWishlist(currentUser.getId(), itemId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, wishlist));
        } catch (IllegalArgumentException ex) {
            log.warn("Remove wishlist item {} failed for user: {} - {}", itemId, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Remove wishlist item {} error for user: {}", itemId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't remove item from wishlist due to some issue."));
        }
    }
}
