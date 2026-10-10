package com.xpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fields a user may change on a savings goal. Missing fields are left as they are. */
public class GoalUpdateRequest {

    private String title;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private String icon;
    private String category;
    private Boolean isEmergency;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsEmergency() { return isEmergency; }
    public void setIsEmergency(Boolean isEmergency) { this.isEmergency = isEmergency; }
}
