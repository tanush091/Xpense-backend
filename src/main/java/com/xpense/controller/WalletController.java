package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.TopUpRequest;
import com.xpense.model.Wallet;
import com.xpense.security.SecurityUtils;
import com.xpense.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Wallet>>> getWallets() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        List<Wallet> wallets = walletService.getWalletsByUserId(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(wallets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Wallet>> getWalletById(@PathVariable String id) {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Wallet>> createWallet(@Valid @RequestBody Wallet wallet) {
        wallet.setUserId(SecurityUtils.getAuthenticatedUserId());
        Wallet created = walletService.createWallet(wallet);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Wallet created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Wallet>> updateWallet(@PathVariable String id, @RequestBody Wallet wallet) {
        Wallet updated = walletService.updateWallet(id, wallet);
        return ResponseEntity.ok(ApiResponse.success("Wallet updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWallet(@PathVariable String id) {
        walletService.deleteWallet(id);
        return ResponseEntity.ok(ApiResponse.success("Wallet deleted successfully", null));
    }

    @PostMapping("/{id}/topup")
    public ResponseEntity<ApiResponse<Wallet>> topUpWallet(
            @PathVariable String id,
            @Valid @RequestBody TopUpRequest request) {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        Wallet updated = walletService.topUpWallet(id, effectiveUserId, request.getAmount(),
                Boolean.TRUE.equals(request.getFromAvailable()));
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully", updated));
    }
}
