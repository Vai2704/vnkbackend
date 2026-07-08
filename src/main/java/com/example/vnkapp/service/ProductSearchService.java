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
import java.util.Set;
import java.util.UUID;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    public ProductSearchService(ProductRepository productRepository,
                                WishlistRepository wishlistRepository) {
        this.productRepository = productRepository;
        this.wishlistRepository = wishlistRepository;
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

        Page<ProductSummaryDto> results = productRepository
                .searchActiveProducts(query.trim(), pageable)
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())));

        log.info("Search '{}' returned {} results (page {}/{})", query, results.getTotalElements(), page, results.getTotalPages());
        return results;
    }
}
