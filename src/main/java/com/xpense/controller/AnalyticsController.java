package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.AnalyticsSummaryDTO;
import com.xpense.security.SecurityUtils;
import com.xpense.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpense.dto.AlertDTO;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsSummaryDTO>> getAnalytics() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        AnalyticsSummaryDTO summary = analyticsService.getAnalyticsSummary(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getAlerts() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        List<AlertDTO> alerts = analyticsService.getAlerts(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }
}
