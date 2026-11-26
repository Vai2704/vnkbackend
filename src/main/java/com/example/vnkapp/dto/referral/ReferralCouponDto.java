package com.example.vnkapp.dto.referral;

import com.example.vnkapp.entity.Coupon;
import com.example.vnkapp.enums.product.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReferralCouponDto(
        UUID id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant validUntil
) {
    public static ReferralCouponDto fromEntity(Coupon coupon) {
        return new ReferralCouponDto(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getValidUntil()
        );
    }
}
