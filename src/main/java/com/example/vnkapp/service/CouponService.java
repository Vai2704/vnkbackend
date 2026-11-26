package com.example.vnkapp.service;

import com.example.vnkapp.config.ReferralProperties;
import com.example.vnkapp.entity.Coupon;
import com.example.vnkapp.enums.product.DiscountType;
import com.example.vnkapp.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);
    private static final String COUPON_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CouponRepository couponRepository;
    private final ReferralProperties referralProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CouponService(CouponRepository couponRepository, ReferralProperties referralProperties) {
        this.couponRepository = couponRepository;
        this.referralProperties = referralProperties;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(UUID couponId, UUID userId, BigDecimal subtotal) {
        Coupon coupon = validateCouponForUser(couponId, userId, subtotal);
        return calculateDiscountAmount(coupon, subtotal);
    }

    @Transactional
    public void markCouponUsed(UUID couponId) {
        couponRepository.findByIdActive(couponId).ifPresent(coupon -> {
            coupon.setTimesUsed(coupon.getTimesUsed() + 1);
            couponRepository.save(coupon);
            log.info("Coupon {} marked as used (timesUsed={})", coupon.getCode(), coupon.getTimesUsed());
        });
    }

    @Transactional(readOnly = true)
    public List<Coupon> getActiveCouponsForUser(UUID userId) {
        return couponRepository.findByAssignedUserIdActive(userId).stream()
                .filter(Coupon::isValid)
                .toList();
    }

    @Transactional
    public Coupon createRefereeWelcomeCoupon(UUID userId, String referrerMemberId) {
        return createReferralCoupon(
                userId,
                referralProperties.getRefereeDiscountPercent(),
                referralProperties.getRefereeCouponValidityDays(),
                "Referral welcome discount (" + referrerMemberId + ")",
                "REF-WEL-"
        );
    }

    @Transactional
    public Coupon createReferrerRewardCoupon(UUID userId) {
        return createReferralCoupon(
                userId,
                referralProperties.getReferrerRewardPercent(),
                referralProperties.getReferrerCouponValidityDays(),
                "Referral reward for a successful invite",
                "REF-RWD-"
        );
    }

    private Coupon createReferralCoupon(UUID userId,
                                        int discountPercent,
                                        int validityDays,
                                        String description,
                                        String codePrefix) {
        Instant now = Instant.now();
        Coupon coupon = Coupon.builder()
                .code(generateUniqueCouponCode(codePrefix))
                .description(description)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(discountPercent))
                .minOrderAmount(referralProperties.getMinOrderAmount())
                .usageLimit(1)
                .usageLimitPerUser(1)
                .timesUsed(0)
                .validFrom(now)
                .validUntil(now.plus(validityDays, ChronoUnit.DAYS))
                .assignedUserId(userId)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Created referral coupon {} for user {}", saved.getCode(), userId);
        return saved;
    }

    private Coupon validateCouponForUser(UUID couponId, UUID userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByIdActive(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is expired or no longer valid");
        }

        if (coupon.getAssignedUserId() != null && !coupon.getAssignedUserId().equals(userId)) {
            throw new IllegalArgumentException("This coupon does not belong to your account");
        }

        if (coupon.getMinOrderAmount() != null
                && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order amount of " + coupon.getMinOrderAmount() + " is required for this coupon");
        }

        return coupon;
    }

    private BigDecimal calculateDiscountAmount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null
                    && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(subtotal) > 0) {
            return subtotal;
        }
        return discount;
    }

    private String generateUniqueCouponCode(String prefix) {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 8; i++) {
                sb.append(COUPON_ALPHABET.charAt(secureRandom.nextInt(COUPON_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (!couponRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique coupon code");
    }
}
