package com.xpense.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TopUpRequest {

    @NotNull(message = "Enter an amount.")
    @DecimalMin(value = "0.01", message = "Enter an amount greater than zero.")
    private BigDecimal amount;

    /**
     * When true, the money is moved out of the user's unbudgeted balance
     * (total balance minus all wallet balances) instead of being added as new money.
     */
    @JsonAlias({"fromAvailable", "from_available"})
    private Boolean fromAvailable = true;

    public TopUpRequest() {
    }

    public TopUpRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Boolean getFromAvailable() {
        return fromAvailable;
    }

    public void setFromAvailable(Boolean fromAvailable) {
        this.fromAvailable = fromAvailable;
    }
}
