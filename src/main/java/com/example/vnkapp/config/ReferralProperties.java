package com.example.vnkapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.referral")
@Getter
@Setter
public class ReferralProperties {

    /** Percentage discount for the referred user on their first order. */
    private int refereeDiscountPercent = 10;

    /** Days the referee welcome coupon stays valid. */
    private int refereeCouponValidityDays = 30;

    /** Percentage discount rewarded to the referrer after referee's first paid order. */
    private int referrerRewardPercent = 10;

    /** Days the referrer reward coupon stays valid. */
    private int referrerCouponValidityDays = 30;

    /** Optional minimum order amount to apply referral coupons. */
    private BigDecimal minOrderAmount = BigDecimal.valueOf(100);
}
