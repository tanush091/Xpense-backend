package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.BillPaymentDTO;
import com.xpense.dto.BillRequest;
import com.xpense.model.RecurringBill;
import com.xpense.security.SecurityUtils;
import com.xpense.service.RecurringBillService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class RecurringBillController {

    private final RecurringBillService billService;

    public RecurringBillController(RecurringBillService billService) {
        this.billService = billService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringBill>>> getBills() {
        return ResponseEntity.ok(ApiResponse.success(billService.getActiveBills(SecurityUtils.getAuthenticatedUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringBill>> createBill(@RequestBody BillRequest bill) {
        RecurringBill created = billService.createBill(SecurityUtils.getAuthenticatedUserId(), bill);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bill added", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBill>> updateBill(@PathVariable String id, @RequestBody BillRequest bill) {
        RecurringBill updated = billService.updateBill(id, SecurityUtils.getAuthenticatedUserId(), bill);
        return ResponseEntity.ok(ApiResponse.success("Bill updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBill(@PathVariable String id) {
        billService.deleteBill(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success("Bill removed", null));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<BillPaymentDTO>> payBill(@PathVariable String id) {
        BillPaymentDTO result = billService.payBill(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success("Bill paid", result));
    }
}
