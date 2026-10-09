package com.xpense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GoalDepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Deposit amount must be at least 1.00")
    private BigDecimal amount;

    public GoalDepositRequest() {
    }

    public GoalDepositRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
