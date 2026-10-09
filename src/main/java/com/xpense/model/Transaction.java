package com.xpense.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    @JsonProperty("user_id")
    @JsonAlias({"userId", "user_id"})
    private String userId;

    @Column(name = "wallet_id", length = 64)
    @JsonProperty("wallet_id")
    @JsonAlias({"walletId", "wallet_id"})
    private String walletId;

    @Column(name = "wallet_name")
    @JsonProperty("wallet_name")
    @JsonAlias({"walletName", "wallet_name"})
    private String walletName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String type; // 'income', 'expense', 'transfer'

    @Column(length = 64)
    private String category = "General";

    @Column
    private String recipient;

    @Column
    private String merchant;

    @Column(name = "payment_method")
    @JsonProperty("payment_method")
    @JsonAlias({"paymentMethod", "payment_method"})
    private String paymentMethod = "UPI";

    @Column(length = 32)
    private String status = "completed";

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {
    }

    public Transaction(String id, String userId, String walletId, String walletName, String title, BigDecimal amount, String type, String category, String merchant, String recipient) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.walletId = walletId;
        this.walletName = walletName;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.merchant = merchant;
        this.recipient = recipient;
        this.date = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        if (date == null) {
            date = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("userId")
    public String getUserIdCamel() {
        return userId;
    }

    @JsonProperty("user_id")
    @JsonAlias({"userId", "user_id"})
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("wallet_id")
    public String getWalletId() {
        return walletId;
    }

    @JsonProperty("walletId")
    public String getWalletIdCamel() {
        return walletId;
    }

    @JsonProperty("wallet_id")
    @JsonAlias({"walletId", "wallet_id"})
    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    @JsonProperty("wallet_name")
    public String getWalletName() {
        return walletName;
    }

    @JsonProperty("walletName")
    public String getWalletNameCamel() {
        return walletName;
    }

    @JsonProperty("wallet_name")
    @JsonAlias({"walletName", "wallet_name"})
    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    @JsonProperty("payment_method")
    public String getPaymentMethod() {
        return paymentMethod;
    }

    @JsonProperty("paymentMethod")
    public String getPaymentMethodCamel() {
        return paymentMethod;
    }

    @JsonProperty("payment_method")
    @JsonAlias({"paymentMethod", "payment_method"})
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @JsonProperty("created_at")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAtCamel() {
        return createdAt;
    }

    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
