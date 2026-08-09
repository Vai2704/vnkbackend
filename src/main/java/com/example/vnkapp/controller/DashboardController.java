package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.dashboard.DashboardResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("Get dashboard request");
        try {
            UUID userId = currentUser != null ? currentUser.getId() : null;
            DashboardResponseDto dashboard = dashboardService.getDashboard(userId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, dashboard));
        } catch (Exception ex) {
            log.error("Get dashboard error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch dashboard due to some issue."));
        }
    }
}
