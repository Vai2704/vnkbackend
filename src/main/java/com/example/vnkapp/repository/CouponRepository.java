package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByIdAndStatus(UUID id, Integer status);

    default Optional<Coupon> findByIdActive(UUID id) {
        return findByIdAndStatus(id, BaseEntity.STATUS_ACTIVE);
    }

    List<Coupon> findByAssignedUserIdAndStatus(UUID assignedUserId, Integer status);

    default List<Coupon> findByAssignedUserIdActive(UUID assignedUserId) {
        return findByAssignedUserIdAndStatus(assignedUserId, BaseEntity.STATUS_ACTIVE);
    }

    boolean existsByCode(String code);
}
