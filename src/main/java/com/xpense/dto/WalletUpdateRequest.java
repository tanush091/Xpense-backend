package com.xpense.dto;

import java.math.BigDecimal;

/** Fields a user may change on a budget. Every field is optional; missing fields are left as they are. */
public class WalletUpdateRequest {

    private String name;
    private String category;
    private BigDecimal budgetLimit;
    private String icon;
    private String color;
    private Boolean isTaxReserve;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal budgetLimit) { this.budgetLimit = budgetLimit; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Boolean getIsTaxReserve() { return isTaxReserve; }
    public void setIsTaxReserve(Boolean isTaxReserve) { this.isTaxReserve = isTaxReserve; }
}
