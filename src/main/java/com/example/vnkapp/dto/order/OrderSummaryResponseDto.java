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
        Integer itemCount,
        Instant createdAt
) {
    public static OrderSummaryResponseDto fromEntity(Order order, Integer itemCount) {
        return new OrderSummaryResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                itemCount,
                order.getCreatedAt()
        );
    }
}
