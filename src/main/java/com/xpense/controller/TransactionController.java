package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.TransferRequest;
import com.xpense.model.Transaction;
import com.xpense.security.SecurityUtils;
import com.xpense.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactions(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "walletId", required = false) String walletId) {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        List<Transaction> transactions = transactionService.getTransactions(effectiveUserId, category, type, walletId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionById(@PathVariable String id) {
        Transaction tx = transactionService.getOwnedTransaction(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success(tx));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> createTransaction(@Valid @RequestBody Transaction transaction) {
        transaction.setUserId(SecurityUtils.getAuthenticatedUserId());
        Transaction created = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Saved", created));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> processTransfer(
            @Valid @RequestBody TransferRequest transferRequest) {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        Transaction tx = transactionService.processTransfer(effectiveUserId, transferRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Money sent", tx));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable String id) {
        transactionService.deleteTransaction(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success("Removed from your activity", null));
    }
}
