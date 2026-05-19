package com.example.vnkapp.entity;

import com.example.vnkapp.enums.referral.ReferralStatus;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referrals_referrer_id", columnList = "referrer_id"),
    @Index(name = "idx_referrals_referred_id", columnList = "referred_id"),
    @Index(name = "idx_referrals_referral_code_id", columnList = "referral_code_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral extends BaseEntity {

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referred_id", nullable = false, unique = true)
    private UUID referredId;

    @Column(name = "referral_code_id", nullable = false)
    private UUID referralCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_status", nullable = false)
    @Builder.Default
    private ReferralStatus referralStatus = ReferralStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "first_order_id")
    private UUID firstOrderId;
}
