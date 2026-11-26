package com.example.vnkapp.service;

import com.example.vnkapp.dto.referral.ReferralCouponDto;
import com.example.vnkapp.dto.referral.ReferralSummaryDto;
import com.example.vnkapp.entity.Coupon;
import com.example.vnkapp.entity.Referral;
import com.example.vnkapp.entity.ReferralCode;
import com.example.vnkapp.entity.ReferralReward;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.enums.referral.ReferralStatus;
import com.example.vnkapp.enums.referral.RewardStatus;
import com.example.vnkapp.enums.referral.RewardType;
import com.example.vnkapp.repository.OrderRepository;
import com.example.vnkapp.repository.ReferralCodeRepository;
import com.example.vnkapp.repository.ReferralRepository;
import com.example.vnkapp.repository.ReferralRewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralRepository referralRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final OrderRepository orderRepository;
    private final CouponService couponService;

    public ReferralService(ReferralCodeRepository referralCodeRepository,
                           ReferralRepository referralRepository,
                           ReferralRewardRepository referralRewardRepository,
                           OrderRepository orderRepository,
                           CouponService couponService) {
        this.referralCodeRepository = referralCodeRepository;
        this.referralRepository = referralRepository;
        this.referralRewardRepository = referralRewardRepository;
        this.orderRepository = orderRepository;
        this.couponService = couponService;
    }

    /**
     * Links a newly registered user to a referrer via Member ID and issues a welcome discount coupon.
     *
     * @return the welcome coupon code, or null if no member ID was provided
     */
    @Transactional
    public String processReferralSignup(UUID referredUserId, String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return null;
        }

        String normalizedMemberId = memberId.trim().toUpperCase();

        ReferralCode referrerCode = referralCodeRepository.findByCode(normalizedMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid member ID"));

        if (referrerCode.getUserId().equals(referredUserId)) {
            throw new IllegalArgumentException("You cannot use your own member ID");
        }

        if (referralRepository.findByReferredId(referredUserId).isPresent()) {
            throw new IllegalArgumentException("Referral has already been applied to this account");
        }

        Referral referral = Referral.builder()
                .referrerId(referrerCode.getUserId())
                .referredId(referredUserId)
                .referralCodeId(referrerCode.getId())
                .referralStatus(ReferralStatus.PENDING)
                .build();
        referralRepository.save(referral);

        referrerCode.setTotalReferrals(referrerCode.getTotalReferrals() + 1);
        referralCodeRepository.save(referrerCode);

        Coupon welcomeCoupon = couponService.createRefereeWelcomeCoupon(referredUserId, normalizedMemberId);

        ReferralReward refereeReward = ReferralReward.builder()
                .userId(referredUserId)
                .referralId(referral.getId())
                .rewardType(RewardType.DISCOUNT_COUPON)
                .amount(BigDecimal.valueOf(welcomeCoupon.getDiscountValue().intValue()))
                .description("Welcome discount for signing up with member ID " + normalizedMemberId)
                .rewardStatus(RewardStatus.CREDITED)
                .creditedAt(Instant.now())
                .expiresAt(welcomeCoupon.getValidUntil())
                .couponId(welcomeCoupon.getId())
                .build();
        referralRewardRepository.save(refereeReward);

        log.info("Referral signup recorded: referrer={}, referred={}, memberId={}",
                referrerCode.getUserId(), referredUserId, normalizedMemberId);

        return welcomeCoupon.getCode();
    }

    @Transactional
    public void completeReferralOnFirstPaidOrder(UUID referredUserId, UUID orderId) {
        Referral referral = referralRepository
                .findByReferredIdAndReferralStatus(referredUserId, ReferralStatus.PENDING)
                .orElse(null);

        if (referral == null) {
            return;
        }

        long confirmedOrders = orderRepository.countByUserIdAndOrderStatusAndStatus(
                referredUserId, OrderStatus.CONFIRMED, com.example.vnkapp.entity.BaseEntity.STATUS_ACTIVE);

        if (confirmedOrders != 1) {
            return;
        }

        referral.setReferralStatus(ReferralStatus.COMPLETED);
        referral.setCompletedAt(Instant.now());
        referral.setFirstOrderId(orderId);
        referralRepository.save(referral);

        ReferralCode referrerCode = referralCodeRepository.findById(referral.getReferralCodeId())
                .orElseThrow(() -> new IllegalStateException("Referral code not found for referral " + referral.getId()));

        referrerCode.setSuccessfulReferrals(referrerCode.getSuccessfulReferrals() + 1);
        referralCodeRepository.save(referrerCode);

        Coupon referrerCoupon = couponService.createReferrerRewardCoupon(referral.getReferrerId());

        ReferralReward referrerReward = ReferralReward.builder()
                .userId(referral.getReferrerId())
                .referralId(referral.getId())
                .rewardType(RewardType.DISCOUNT_COUPON)
                .amount(BigDecimal.valueOf(referrerCoupon.getDiscountValue().intValue()))
                .description("Reward for successful referral")
                .rewardStatus(RewardStatus.CREDITED)
                .creditedAt(Instant.now())
                .expiresAt(referrerCoupon.getValidUntil())
                .couponId(referrerCoupon.getId())
                .build();
        referralRewardRepository.save(referrerReward);

        log.info("Referral completed: referrer={}, referred={}, order={}",
                referral.getReferrerId(), referredUserId, orderId);
    }

    @Transactional(readOnly = true)
    public ReferralSummaryDto getReferralSummary(UUID userId) {
        ReferralCode referralCode = referralCodeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member ID not found for user"));

        List<ReferralCouponDto> coupons = couponService.getActiveCouponsForUser(userId).stream()
                .map(ReferralCouponDto::fromEntity)
                .toList();

        return new ReferralSummaryDto(
                referralCode.getCode(),
                referralCode.getTotalReferrals(),
                referralCode.getSuccessfulReferrals(),
                coupons
        );
    }

    @Transactional(readOnly = true)
    public String getMemberId(UUID userId) {
        return referralCodeRepository.findByUserId(userId)
                .map(ReferralCode::getCode)
                .orElse(null);
    }
}
