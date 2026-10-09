package com.xpense.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    @JsonProperty("user_id")
    @JsonAlias({"userId", "user_id"})
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "budget_limit", precision = 12, scale = 2)
    @JsonProperty("budget_limit")
    @JsonAlias({"budgetLimit", "budget_limit"})
    private BigDecimal budgetLimit = new BigDecimal("1000.00");

    @Column
    private String icon = "wallet";

    @Column
    private String color = "#3B82F6";

    @Column(name = "cycle_days_left")
    @JsonProperty("cycle_days_left")
    @JsonAlias({"cycleDaysLeft", "cycle_days_left"})
    private Integer cycleDaysLeft = 30;

    @Column(name = "daily_avg", precision = 10, scale = 2)
    @JsonProperty("daily_avg")
    @JsonAlias({"dailyAvg", "daily_avg"})
    private BigDecimal dailyAvg = BigDecimal.ZERO;

    @Column
    private String status = "Good";

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    @JsonAlias({"updatedAt", "updated_at"})
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Wallet() {
    }

    public Wallet(String id, String userId, String name, String category, BigDecimal balance, BigDecimal budgetLimit, String icon, String color) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.balance = balance;
        this.budgetLimit = budgetLimit;
        this.icon = icon;
        this.color = color;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @JsonProperty("budget_limit")
    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    @JsonProperty("budgetLimit")
    public BigDecimal getBudgetLimitCamel() {
        return budgetLimit;
    }

    @JsonProperty("budget_limit")
    @JsonAlias({"budgetLimit", "budget_limit"})
    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty("cycle_days_left")
    public Integer getCycleDaysLeft() {
        return cycleDaysLeft;
    }

    @JsonProperty("cycleDaysLeft")
    public Integer getCycleDaysLeftCamel() {
        return cycleDaysLeft;
    }

    @JsonProperty("cycle_days_left")
    @JsonAlias({"cycleDaysLeft", "cycle_days_left"})
    public void setCycleDaysLeft(Integer cycleDaysLeft) {
        this.cycleDaysLeft = cycleDaysLeft;
    }

    @JsonProperty("daily_avg")
    public BigDecimal getDailyAvg() {
        return dailyAvg;
    }

    @JsonProperty("dailyAvg")
    public BigDecimal getDailyAvgCamel() {
        return dailyAvg;
    }

    @JsonProperty("daily_avg")
    @JsonAlias({"dailyAvg", "daily_avg"})
    public void setDailyAvg(BigDecimal dailyAvg) {
        this.dailyAvg = dailyAvg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @JsonProperty("updated_at")
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty("updatedAt")
    public LocalDateTime getUpdatedAtCamel() {
        return updatedAt;
    }

    @JsonProperty("updated_at")
    @JsonAlias({"updatedAt", "updated_at"})
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
