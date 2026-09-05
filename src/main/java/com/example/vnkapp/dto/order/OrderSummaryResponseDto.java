package com.example.vnkapp.dto.order;

import com.example.vnkapp.entity.Order;
import com.example.vnkapp.enums.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponseDto(
        UUID id,
        String orderNumber,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        String currencySymbol,
        Integer itemCount,
        String thumbnailImage,
        Instant createdAt
) {
    public static OrderSummaryResponseDto fromEntity(Order order, Integer itemCount) {
        return fromEntity(order, itemCount, null);
    }

    public static OrderSummaryResponseDto fromEntity(Order order, Integer itemCount, String thumbnailImage) {
        return new OrderSummaryResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                resolveCurrencySymbol(order.getCurrencySymbol()),
                itemCount,
                thumbnailImage,
                order.getCreatedAt()
        );
    }

    private static String resolveCurrencySymbol(String currencySymbol) {
        if (currencySymbol == null || currencySymbol.isBlank()) {
            return "AED";
        }
        return currencySymbol;
    }
}
