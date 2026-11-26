package com.example.vnkapp.dto.referral;

import java.util.List;

public record ReferralSummaryDto(
        String memberId,
        int totalReferrals,
        int successfulReferrals,
        List<ReferralCouponDto> availableCoupons
) {}
