package com.example.vnkapp.service;

import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductImage;
import com.example.vnkapp.repository.ProductImageRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductThumbnailService {

    private final ProductImageRepository productImageRepository;

    public ProductThumbnailService(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    public Map<UUID, String> thumbnailsFor(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ids = productIds.stream().distinct().toList();
        return productImageRepository.findPrimaryByProductIds(ids).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, this::resolve, (a, b) -> a));
    }

    public String thumbnailOrFallback(UUID productId, String fallback) {
        String thumbnail = productImageRepository.findPrimaryByProductId(productId)
                .map(this::resolve)
                .orElse(null);
        return firstNonBlank(thumbnail, fallback);
    }

    public String resolve(ProductImage image) {
        if (image == null) {
            return null;
        }
        return firstNonBlank(image.getThumbnailUrl(), image.getImageUrl());
    }

    public String fromProduct(Product product) {
        if (product == null || product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return null;
        }
        return product.getImageUrls().get(0);
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
