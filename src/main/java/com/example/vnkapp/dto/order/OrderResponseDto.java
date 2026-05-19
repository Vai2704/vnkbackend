package com.example.vnkapp.dto.order;

import com.example.vnkapp.entity.Order;
import com.example.vnkapp.enums.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        String orderNumber,
        OrderStatus orderStatus,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String shippingFullName,
        String shippingPhone,
        String shippingAddress,
        String shippingCity,
        String shippingState,
        String shippingPostalCode,
        String shippingCountry,
        String notes,
        String cancellationReason,
        Instant cancelledAt,
        Instant shippedAt,
        Instant deliveredAt,
        String trackingNumber,
        String trackingUrl,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponseDto> items
) {
    public static OrderResponseDto fromEntity(Order order, List<OrderItemResponseDto> items) {
        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getShippingFullName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getNotes(),
                order.getCancellationReason(),
                order.getCancelledAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getTrackingNumber(),
                order.getTrackingUrl(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
