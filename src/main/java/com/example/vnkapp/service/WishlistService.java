package com.example.vnkapp.service;

import com.example.vnkapp.dto.wishlist.AddToWishlistRequestDto;
import com.example.vnkapp.dto.wishlist.WishlistItemResponseDto;
import com.example.vnkapp.dto.wishlist.WishlistResponseDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.Wishlist;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public WishlistResponseDto addToWishlist(UUID userId, AddToWishlistRequestDto dto) {
        // Validate product exists
        productRepository.findByIdAndStatusActive(dto.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductIdActive(userId, dto.productId())) {
            throw new IllegalArgumentException("Product already in wishlist");
        }

        // Add to wishlist
        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .productId(dto.productId())
                .build();
        wishlistRepository.save(wishlist);

        return getWishlist(userId);
    }

    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlist(UUID userId) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdActive(userId);

        if (wishlistItems.isEmpty()) {
            return new WishlistResponseDto(List.of(), 0);
        }

        // Fetch all products in one query
        List<UUID> productIds = wishlistItems.stream()
                .map(Wishlist::getProductId)
                .toList();

        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<WishlistItemResponseDto> items = wishlistItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return new WishlistItemResponseDto(
                            item.getId(),
                            item.getProductId(),
                            product != null ? product.getName() : null,
                            product != null ? product.getSlug() : null,
                            product != null ? product.getPrice() : null,
                            product != null && product.getStockQuantity() > 0,
                            item.getCreatedAt()
                    );
                })
                .toList();

        return new WishlistResponseDto(items, items.size());
    }

    @Transactional
    public WishlistResponseDto removeFromWishlist(UUID userId, UUID wishlistItemId) {
        Wishlist wishlistItem = wishlistRepository.findById(wishlistItemId)
                .filter(item -> item.getUserId().equals(userId) && item.getStatus().equals(BaseEntity.STATUS_ACTIVE))
                .orElseThrow(() -> new IllegalArgumentException("Wishlist item not found"));

        // Soft delete
        wishlistItem.setStatus(BaseEntity.STATUS_INACTIVE);
        wishlistRepository.save(wishlistItem);

        return getWishlist(userId);
    }
}
