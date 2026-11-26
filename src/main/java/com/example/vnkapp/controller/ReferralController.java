package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.referral.ReferralSummaryDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.ReferralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private static final Logger log = LoggerFactory.getLogger(ReferralController.class);

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyReferralSummary(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get referral summary for user: {}", currentUser.getId());
        try {
            ReferralSummaryDto summary = referralService.getReferralSummary(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, summary));
        } catch (IllegalArgumentException ex) {
            log.warn("Referral summary failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Referral summary error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch referral details due to some issue."));
        }
    }
}
