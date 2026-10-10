package com.xpense.dto;

import java.math.BigDecimal;

/** Who money was paid to in a period, with the total and share of all spending. */
public record PayeeDTO(String name, BigDecimal amount, int percent, int payments) {
}
