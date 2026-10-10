package com.xpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for creating or editing a bill. On edit, missing fields are left as they are. */
public class BillRequest {

    private String name;
    private BigDecimal amount;
    private String frequency;
    private LocalDate nextDueDate;
    /** A wallet id, or "" for "money not in a budget". Null means "don't change". */
    private String walletId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
}
