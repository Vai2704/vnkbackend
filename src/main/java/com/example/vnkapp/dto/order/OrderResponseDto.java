package com.example.vnkapp.dto.order;

import com.example.vnkapp.entity.Order;
import com.example.vnkapp.entity.Payment;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.enums.payment.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        String currencySymbol,
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
        List<OrderItemResponseDto> items,
        PaymentStatus paymentStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String paymentUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String paymentMessage
) {
    public static OrderResponseDto fromEntity(Order order, List<OrderItemResponseDto> items, Payment payment) {
        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                resolveCurrencySymbol(order.getCurrencySymbol()),
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
                items,
                payment != null ? payment.getPaymentStatus() : null,
                paymentUrlFor(order, payment),
                paymentMessageFor(order, payment)
        );
    }

    private static String paymentUrlFor(Order order, Payment payment) {
        if (order.getOrderStatus() != OrderStatus.PENDING || payment == null) {
            return null;
        }
        return payment.getGatewayPaymentUrl();
    }

    private static String paymentMessageFor(Order order, Payment payment) {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return null;
        }
        PaymentStatus status = payment != null ? payment.getPaymentStatus() : PaymentStatus.PENDING;
        return switch (status) {
            case FAILED -> "Payment failed. Please retry payment to confirm this order.";
            case PROCESSING -> "Payment is being processed. Order details are shown below.";
            case COMPLETED -> null;
            default -> "Payment is pending. Please complete payment to confirm this order.";
        };
    }

    private static String resolveCurrencySymbol(String currencySymbol) {
        if (currencySymbol == null || currencySymbol.isBlank()) {
            return "AED";
        }
        return currencySymbol;
    }
}
