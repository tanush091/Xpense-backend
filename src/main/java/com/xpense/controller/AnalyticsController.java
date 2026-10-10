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
import com.xpense.dto.MonthlyTotalDTO;
import com.xpense.dto.PayeeDTO;
import com.xpense.exception.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    /** Money in vs money out per month. months is limited to 1–24. */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<MonthlyTotalDTO>>> getMonthlyTotals(
            @RequestParam(value = "months", defaultValue = "6") int months) {
        int safeMonths = Math.max(1, Math.min(24, months));
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getMonthlyTotals(SecurityUtils.getAuthenticatedUserId(), safeMonths)));
    }

    /** Biggest payees for a date range (defaults to this month). limit is 1–50. */
    @GetMapping("/payees")
    public ResponseEntity<ApiResponse<List<PayeeDTO>>> getTopPayees(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        LocalDate start;
        LocalDate end;
        try {
            start = from != null && !from.isBlank() ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
            end = to != null && !to.isBlank() ? LocalDate.parse(to) : LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Dates must look like 2026-10-01.");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("The end date must be after the start date.");
        }
        int safeLimit = Math.max(1, Math.min(50, limit));
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTopPayees(SecurityUtils.getAuthenticatedUserId(), start, end, safeLimit)));
    }
}
