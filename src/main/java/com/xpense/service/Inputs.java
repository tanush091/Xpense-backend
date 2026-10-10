package com.xpense.service;

import com.xpense.exception.BadRequestException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Shared input checks so every endpoint rejects bad values with the same plain message. */
public final class Inputs {

    /** ₹100 crore: far above any real personal or small-business amount, well inside NUMERIC(12,2). */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private Inputs() {
    }

    public static void requireAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Enter an amount greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("Amounts can have at most 2 decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BadRequestException("That amount is too large. The most Xpense accepts is ₹100,00,00,000.");
        }
    }

    /** Trims text and rejects it if it is longer than {@code max} characters. Null stays null. */
    public static String text(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BadRequestException(label + " is too long. Keep it under " + max + " characters.");
        }
        return trimmed;
    }

    /** Like {@link #text} but the value must not be empty. */
    public static String requiredText(String value, int max, String label, String emptyMessage) {
        String trimmed = text(value, max, label);
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BadRequestException(emptyMessage);
        }
        return trimmed;
    }

    /** ₹1,24,500 or ₹10.50 — the same money format the app shows. */
    public static String inr(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        boolean whole = v.stripTrailingZeros().scale() <= 0;
        NumberFormat f = NumberFormat.getNumberInstance(Locale.of("en", "IN"));
        f.setMinimumFractionDigits(whole ? 0 : 2);
        f.setMaximumFractionDigits(whole ? 0 : 2);
        return "₹" + f.format(v);
    }
}
