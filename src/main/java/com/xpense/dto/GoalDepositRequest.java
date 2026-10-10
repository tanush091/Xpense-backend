package com.xpense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GoalDepositRequest {

    @NotNull(message = "Enter an amount.")
    @DecimalMin(value = "0.01", message = "Enter an amount greater than zero.")
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
