package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Order;
import com.example.vnkapp.enums.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Integer status, Pageable pageable);

    default Page<Order> findByUserIdActivePaginated(UUID userId, Pageable pageable) {
        return findByUserIdAndStatusOrderByCreatedAtDesc(userId, BaseEntity.STATUS_ACTIVE, pageable);
    }

    Page<Order> findByUserIdAndOrderStatusAndStatusOrderByCreatedAtDesc(
            UUID userId, OrderStatus orderStatus, Integer status, Pageable pageable);

    default Page<Order> findByUserIdAndOrderStatusActivePaginated(
            UUID userId, OrderStatus orderStatus, Pageable pageable) {
        return findByUserIdAndOrderStatusAndStatusOrderByCreatedAtDesc(
                userId, orderStatus, BaseEntity.STATUS_ACTIVE, pageable);
    }

    Optional<Order> findByIdAndUserIdAndStatus(UUID id, UUID userId, Integer status);

    default Optional<Order> findByIdAndUserIdActive(UUID id, UUID userId) {
        return findByIdAndUserIdAndStatus(id, userId, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsByOrderNumber(String orderNumber);
}
