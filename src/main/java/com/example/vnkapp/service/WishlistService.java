package com.example.vnkapp.service;

import com.example.vnkapp.dto.wishlist.AddToWishlistRequestDto;
import com.example.vnkapp.dto.wishlist.WishlistItemResponseDto;
import com.example.vnkapp.dto.wishlist.WishlistResponseDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.Wishlist;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistService.class);

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public WishlistResponseDto addToWishlist(UUID userId, AddToWishlistRequestDto dto) {
        log.debug("Adding product {} to wishlist for user: {}", dto.productId(), userId);
        // Validate product exists
        productRepository.findByIdAndStatusActive(dto.productId())
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", dto.productId());
                    return new IllegalArgumentException("Product not found");
                });

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductIdActive(userId, dto.productId())) {
            log.warn("Product {} already in wishlist for user: {}", dto.productId(), userId);
            throw new IllegalArgumentException("Product already in wishlist");
        }

        // Add to wishlist
        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .productId(dto.productId())
                .build();
        wishlistRepository.save(wishlist);
        log.info("Product {} added to wishlist for user: {}", dto.productId(), userId);

        return getWishlist(userId);
    }

    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlist(UUID userId) {
        log.debug("Fetching wishlist for user: {}", userId);
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
        log.debug("Removing wishlist item {} for user: {}", wishlistItemId, userId);
        Wishlist wishlistItem = wishlistRepository.findById(wishlistItemId)
                .filter(item -> item.getUserId().equals(userId) && item.getStatus().equals(BaseEntity.STATUS_ACTIVE))
                .orElseThrow(() -> {
                    log.warn("Wishlist item {} not found for user: {}", wishlistItemId, userId);
                    return new IllegalArgumentException("Wishlist item not found");
                });

        // Soft delete
        wishlistItem.setStatus(BaseEntity.STATUS_INACTIVE);
        wishlistRepository.save(wishlistItem);
        log.info("Wishlist item {} removed for user: {}", wishlistItemId, userId);

        return getWishlist(userId);
    }
}
