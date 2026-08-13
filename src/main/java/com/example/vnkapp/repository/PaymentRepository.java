package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderIdAndStatus(UUID orderId, Integer status);

    default Optional<Payment> findByOrderIdActive(UUID orderId) {
        return findByOrderIdAndStatus(orderId, BaseEntity.STATUS_ACTIVE);
    }

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
}
