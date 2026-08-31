package com.example.vnkapp.dto.payment;

import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.enums.payment.PaymentStatus;

public record PaymentCallbackResponseDto(
        String orderNumber,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        String gatewayPaymentState,
        String message
) {}
