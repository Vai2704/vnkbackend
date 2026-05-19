package com.example.vnkapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "referral_codes", indexes = {
    @Index(name = "idx_referral_codes_user_id", columnList = "user_id"),
    @Index(name = "idx_referral_codes_code", columnList = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralCode extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(name = "total_referrals")
    @Builder.Default
    private Integer totalReferrals = 0;

    @Column(name = "successful_referrals")
    @Builder.Default
    private Integer successfulReferrals = 0;
}
