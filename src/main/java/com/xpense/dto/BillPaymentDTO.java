package com.xpense.dto;

import com.xpense.model.RecurringBill;
import com.xpense.model.Transaction;

/** Result of paying a bill: the bill with its next due date, and the expense that was recorded. */
public record BillPaymentDTO(RecurringBill bill, Transaction transaction) {
}
