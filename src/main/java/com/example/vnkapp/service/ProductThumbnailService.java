package com.example.vnkapp.service;

import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductImage;
import com.example.vnkapp.repository.ProductImageRepository;
import com.example.vnkapp.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductThumbnailService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductThumbnailService(ProductImageRepository productImageRepository,
                                   ProductRepository productRepository) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    public Map<UUID, String> thumbnailsFor(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ids = productIds.stream().distinct().toList();
        Map<UUID, String> thumbnails = new HashMap<>();
        for (ProductImage image : productImageRepository.findPrimaryByProductIds(ids)) {
            String url = resolve(image);
            if (url != null) {
                thumbnails.putIfAbsent(image.getProductId(), url);
            }
        }

        List<UUID> missing = ids.stream()
                .filter(id -> firstNonBlank(thumbnails.get(id)) == null)
                .toList();
        if (!missing.isEmpty()) {
            productRepository.findAllById(missing).forEach(product -> {
                String fromProduct = fromProduct(product);
                if (fromProduct != null) {
                    thumbnails.put(product.getId(), fromProduct);
                }
            });
        }
        return thumbnails;
    }

    public String thumbnailOrFallback(UUID productId, String fallback) {
        String thumbnail = productImageRepository.findPrimaryByProductId(productId)
                .map(this::resolve)
                .orElse(null);
        if (firstNonBlank(thumbnail) == null) {
            thumbnail = productRepository.findById(productId)
                    .map(this::fromProduct)
                    .orElse(null);
        }
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
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
