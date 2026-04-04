package com.example.vnkapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_rewards", indexes = {
    @Index(name = "idx_referral_rewards_user_id", columnList = "user_id"),
    @Index(name = "idx_referral_rewards_referral_id", columnList = "referral_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralReward extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "referral_id", nullable = false)
    private UUID referralId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RewardStatus status = RewardStatus.PENDING;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_in_order_id")
    private UUID usedInOrderId;

    public enum RewardType {
        CASHBACK, DISCOUNT_COUPON, WALLET_CREDIT, POINTS
    }

    public enum RewardStatus {
        PENDING, CREDITED, USED, EXPIRED, CANCELLED
    }
}
