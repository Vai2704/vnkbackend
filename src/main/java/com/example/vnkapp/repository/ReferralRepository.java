package com.example.vnkapp.repository;

import com.example.vnkapp.entity.Referral;
import com.example.vnkapp.enums.referral.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByReferredId(UUID referredId);

    Optional<Referral> findByReferredIdAndReferralStatus(UUID referredId, ReferralStatus referralStatus);
}
