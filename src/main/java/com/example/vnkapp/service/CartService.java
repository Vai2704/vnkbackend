package com.example.vnkapp.service;

import com.example.vnkapp.dto.cart.AddToCartRequestDto;
import com.example.vnkapp.dto.cart.CartItemResponseDto;
import com.example.vnkapp.dto.cart.CartResponseDto;
import com.example.vnkapp.dto.cart.UpdateCartItemRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Cart;
import com.example.vnkapp.entity.CartItem;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.repository.CartItemRepository;
import com.example.vnkapp.repository.CartRepository;
import com.example.vnkapp.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartResponseDto addToCart(UUID userId, AddToCartRequestDto dto) {
        // Validate product exists
        Product product = productRepository.findByIdAndStatusActive(dto.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Get or create cart for user
        Cart cart = cartRepository.findByUserIdActive(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });

        // Check if product already in cart (including soft-deleted)
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), dto.productId())
                .orElse(null);

        if (cartItem != null) {
            if (cartItem.getStatus().equals(BaseEntity.STATUS_ACTIVE)) {
                // Update quantity for active item
                cartItem.setQuantity(cartItem.getQuantity() + dto.quantity());
            } else {
                // Reactivate soft-deleted item
                cartItem.setStatus(BaseEntity.STATUS_ACTIVE);
                cartItem.setQuantity(dto.quantity());
            }
            cartItem.setUnitPrice(product.getPrice());
            cartItemRepository.save(cartItem);
        } else {
            // Add new item
            cartItem = CartItem.builder()
                    .cartId(cart.getId())
                    .productId(dto.productId())
                    .quantity(dto.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            cartItemRepository.save(cartItem);
        }

        return getCart(userId);
    }

    @Transactional(readOnly = true)
    public CartResponseDto getCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdActive(userId)
                .orElse(null);

        if (cart == null) {
            return new CartResponseDto(null, List.of(), 0, BigDecimal.ZERO);
        }

        List<CartItem> cartItems = cartItemRepository.findByCartIdActive(cart.getId());

        if (cartItems.isEmpty()) {
            return new CartResponseDto(cart.getId(), List.of(), 0, BigDecimal.ZERO);
        }

        // Fetch all products in one query
        List<UUID> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();

        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponseDto> items = cartItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponseDto(
                            item.getId(),
                            item.getProductId(),
                            product != null ? product.getName() : null,
                            product != null ? product.getSlug() : null,
                            item.getUnitPrice(),
                            item.getQuantity(),
                            totalPrice
                    );
                })
                .toList();

        int totalItems = items.stream().mapToInt(CartItemResponseDto::quantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponseDto::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDto(cart.getId(), items, totalItems, totalAmount);
    }

    @Transactional
    public CartResponseDto updateCartItemQuantity(UUID userId, UUID cartItemId, UpdateCartItemRequestDto dto) {
        Cart cart = cartRepository.findByUserIdActive(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCartId().equals(cart.getId()) && item.getStatus().equals(BaseEntity.STATUS_ACTIVE))
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        // Update quantity
        cartItem.setQuantity(dto.quantity());
        cartItemRepository.save(cartItem);

        return getCart(userId);
    }

    @Transactional
    public CartResponseDto removeFromCart(UUID userId, UUID cartItemId) {
        Cart cart = cartRepository.findByUserIdActive(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCartId().equals(cart.getId()) && item.getStatus().equals(BaseEntity.STATUS_ACTIVE))
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        // Soft delete
        cartItem.setStatus(BaseEntity.STATUS_INACTIVE);
        cartItemRepository.save(cartItem);

        return getCart(userId);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdActive(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdActive(cart.getId());

        // Soft delete all items
        cartItems.forEach(item -> {
            item.setStatus(BaseEntity.STATUS_INACTIVE);
            cartItemRepository.save(item);
        });
    }
}
