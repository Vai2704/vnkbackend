package com.example.vnkapp.service;

import com.example.vnkapp.dto.product.ProductSummaryDto;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductThumbnailService productThumbnailService;

    public ProductSearchService(ProductRepository productRepository,
                                WishlistRepository wishlistRepository,
                                ProductThumbnailService productThumbnailService) {
        this.productRepository = productRepository;
        this.wishlistRepository = wishlistRepository;
        this.productThumbnailService = productThumbnailService;
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> search(String query, int page, int size, UUID userId) {
        log.debug("Searching products, query: '{}', page: {}, size: {}, userId: {}", query, page, size, userId);

        if (query == null || query.isBlank()) {
            log.warn("Empty search query received");
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Set<UUID> wishlisted = userId != null
                ? wishlistRepository.findWishlistedProductIds(userId)
                : Collections.emptySet();

        var products = productRepository.searchActiveProducts(query.trim(), pageable);
        Map<UUID, String> thumbnails = productThumbnailService.thumbnailsFor(
                products.getContent().stream().map(p -> p.getId()).toList());

        Page<ProductSummaryDto> results = products.map(p -> ProductSummaryDto.fromEntity(
                p,
                wishlisted.contains(p.getId()),
                firstNonBlank(thumbnails.get(p.getId()), productThumbnailService.fromProduct(p))));

        log.info("Search '{}' returned {} results (page {}/{})", query, results.getTotalElements(), page, results.getTotalPages());
        return results;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
