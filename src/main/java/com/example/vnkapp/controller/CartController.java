package com.example.vnkapp.controller;

import com.example.vnkapp.dto.cart.AddToCartRequestDto;
import com.example.vnkapp.dto.cart.CartItemResponseDto;
import com.example.vnkapp.dto.cart.CartResponseDto;
import com.example.vnkapp.dto.cart.UpdateCartItemRequestDto;
import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.CartService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AddToCartRequestDto request) {
        log.info("Add to cart for user: {}, productId: {}", currentUser.getId(), request.productId());
        try {
            CartResponseDto cart = cartService.addToCart(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, cart));
        } catch (IllegalArgumentException ex) {
            log.warn("Add to cart failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Add to cart error for user: {}, productId: {}", currentUser.getId(), request.productId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add product to cart due to some issue. Team is working on this Sorry for Inconvenience"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get cart for user: {}", currentUser.getId());
        try {
            CartResponseDto cart = cartService.getCart(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, cart));
        } catch (Exception ex) {
            log.error("Get cart error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch cart due to some issue."));
        }
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<?> getCartItem(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID itemId) {
        log.info("Get cart item {} for user: {}", itemId, currentUser.getId());
        try {
            CartItemResponseDto item = cartService.getCartItem(currentUser.getId(), itemId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, item));
        } catch (IllegalArgumentException ex) {
            log.warn("Cart item {} not found for user: {} - {}", itemId, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get cart item {} error for user: {}", itemId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch cart item due to some issue."));
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItemQuantity(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        log.info("Update cart item {} for user: {}", itemId, currentUser.getId());
        try {
            CartResponseDto cart = cartService.updateCartItemQuantity(currentUser.getId(), itemId, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, cart));
        } catch (IllegalArgumentException ex) {
            log.warn("Update cart item {} failed for user: {} - {}", itemId, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Update cart item {} error for user: {}", itemId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update cart item due to some issue."));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeFromCart(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID itemId) {
        log.info("Remove cart item {} for user: {}", itemId, currentUser.getId());
        try {
            CartResponseDto cart = cartService.removeFromCart(currentUser.getId(), itemId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, cart));
        } catch (IllegalArgumentException ex) {
            log.warn("Remove cart item {} failed for user: {} - {}", itemId, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Remove cart item {} error for user: {}", itemId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't remove item from cart due to some issue."));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Clear cart for user: {}", currentUser.getId());
        try {
            cartService.clearCart(currentUser.getId());
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Clear cart failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Clear cart error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't clear cart due to some issue."));
        }
    }
}
