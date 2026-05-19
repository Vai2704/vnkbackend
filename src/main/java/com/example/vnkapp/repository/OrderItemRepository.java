package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdAndStatus(UUID orderId, Integer status);

    default List<OrderItem> findByOrderIdActive(UUID orderId) {
        return findByOrderIdAndStatus(orderId, BaseEntity.STATUS_ACTIVE);
    }
}
