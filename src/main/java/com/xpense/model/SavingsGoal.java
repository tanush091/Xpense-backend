package com.xpense.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    @JsonProperty("user_id")
    @JsonAlias({"userId", "user_id"})
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_amount", nullable = false, precision = 12, scale = 2)
    @JsonProperty("target_amount")
    @JsonAlias({"targetAmount", "target_amount"})
    private BigDecimal targetAmount;

    @Column(name = "current_amount", precision = 12, scale = 2)
    @JsonProperty("current_amount")
    @JsonAlias({"currentAmount", "current_amount"})
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "target_date")
    @JsonProperty("target_date")
    @JsonAlias({"targetDate", "target_date"})
    private LocalDate targetDate;

    @Column
    private String icon = "target";

    @Column
    private String category = "Savings";

    @Column(length = 32)
    private String status = "in_progress"; // 'in_progress', 'completed', 'paused'

    /** Personal dashboard: marks the goal used for "emergency fund cover". */
    @Column(name = "is_emergency")
    @JsonProperty("is_emergency")
    @JsonAlias({"isEmergency", "is_emergency"})
    private Boolean isEmergency = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    @JsonAlias({"updatedAt", "updated_at"})
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SavingsGoal() {
    }

    public SavingsGoal(String id, String userId, String title, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate, String icon, String category) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        this.targetDate = targetDate;
        this.icon = icon != null ? icon : "target";
        this.category = category != null ? category : "Savings";
        this.status = "in_progress";
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("target_amount")
    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    @JsonProperty("targetAmount")
    public BigDecimal getTargetAmountCamel() {
        return targetAmount;
    }

    @JsonProperty("target_amount")
    @JsonAlias({"targetAmount", "target_amount"})
    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    @JsonProperty("current_amount")
    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    @JsonProperty("currentAmount")
    public BigDecimal getCurrentAmountCamel() {
        return currentAmount;
    }

    @JsonProperty("current_amount")
    @JsonAlias({"currentAmount", "current_amount"})
    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    @JsonProperty("target_date")
    public LocalDate getTargetDate() {
        return targetDate;
    }

    @JsonProperty("targetDate")
    public LocalDate getTargetDateCamel() {
        return targetDate;
    }

    @JsonProperty("target_date")
    @JsonAlias({"targetDate", "target_date"})
    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    @JsonProperty("is_emergency")
    public Boolean getIsEmergency() {
        return isEmergency;
    }

    @JsonProperty("is_emergency")
    @JsonAlias({"isEmergency", "is_emergency"})
    public void setIsEmergency(Boolean isEmergency) {
        this.isEmergency = isEmergency;
    }
}
