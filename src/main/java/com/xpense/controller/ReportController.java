package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.model.Transaction;
import com.xpense.security.SecurityUtils;
import com.xpense.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<?> getReport(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "format", defaultValue = "json") String format) {

        String userId = SecurityUtils.getAuthenticatedUserId();
        List<Transaction> transactions = reportService.getReportTransactions(userId, from, to);

        if ("csv".equalsIgnoreCase(format)) {
            String csvContent = reportService.generateCsv(transactions);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"xpense-statement.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvContent);
        }

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
