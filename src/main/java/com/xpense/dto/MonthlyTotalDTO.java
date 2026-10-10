package com.xpense.dto;

import java.math.BigDecimal;

/** Money in and money out for one calendar month, e.g. month = "2026-05", label = "May". */
public record MonthlyTotalDTO(String month, String label, BigDecimal moneyIn, BigDecimal moneyOut) {
}
