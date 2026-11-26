package com.example.vnkapp.repository;

import com.example.vnkapp.entity.ReferralReward;
import com.example.vnkapp.enums.referral.RewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralRewardRepository extends JpaRepository<ReferralReward, UUID> {

    List<ReferralReward> findByUserIdAndRewardStatus(UUID userId, RewardStatus rewardStatus);
}
