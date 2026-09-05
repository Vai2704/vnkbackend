package com.example.vnkapp.dto.order;

import com.example.vnkapp.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDto(
        UUID id,
        UUID productId,
        String productName,
        String productSku,
        String productImageUrl,
        String thumbnailImage,
        Integer quantity,
        BigDecimal unitPrice,
        String currencySymbol,
        BigDecimal totalPrice
) {
    public static OrderItemResponseDto fromEntity(OrderItem item) {
        return fromEntity(item, item.getProductImageUrl());
    }

    public static OrderItemResponseDto fromEntity(OrderItem item, String thumbnailImage) {
        String thumbnail = (thumbnailImage != null && !thumbnailImage.isBlank())
                ? thumbnailImage
                : item.getProductImageUrl();
        return new OrderItemResponseDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductSku(),
                item.getProductImageUrl(),
                thumbnail,
                item.getQuantity(),
                item.getUnitPrice(),
                resolveCurrencySymbol(item.getCurrencySymbol()),
                item.getTotalPrice()
        );
    }

    private static String resolveCurrencySymbol(String currencySymbol) {
        if (currencySymbol == null || currencySymbol.isBlank()) {
            return "AED";
        }
        return currencySymbol;
    }
}
