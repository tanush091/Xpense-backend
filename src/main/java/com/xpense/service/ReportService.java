package com.xpense.service;

import com.xpense.model.Transaction;
import com.xpense.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getReportTransactions(String userId, String fromDate, String toDate) {
        if (fromDate != null && !fromDate.trim().isEmpty() && toDate != null && !toDate.trim().isEmpty()) {
            try {
                LocalDate start = LocalDate.parse(fromDate.trim());
                LocalDate end = LocalDate.parse(toDate.trim());
                LocalDateTime startDateTime = start.atStartOfDay();
                LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
                return transactionRepository.findByUserIdAndDateBetween(userId, startDateTime, endDateTime);
            } catch (Exception ignored) {
            }
        }
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    public String generateCsv(List<Transaction> transactions) {
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("ID,Date,Title,Type,Category,Amount (INR),Wallet,Recipient,Payment Method,Status\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Transaction tx : transactions) {
            String dateStr = tx.getDate() != null ? tx.getDate().format(dtf) : "";
            sb.append(escapeCsv(tx.getId())).append(",");
            sb.append(escapeCsv(dateStr)).append(",");
            sb.append(escapeCsv(tx.getTitle())).append(",");
            sb.append(escapeCsv(tx.getType())).append(",");
            sb.append(escapeCsv(tx.getCategory())).append(",");
            sb.append(tx.getAmount() != null ? tx.getAmount().toPlainString() : "0.00").append(",");
            sb.append(escapeCsv(tx.getWalletName())).append(",");
            sb.append(escapeCsv(tx.getRecipient())).append(",");
            sb.append(escapeCsv(tx.getPaymentMethod())).append(",");
            sb.append(escapeCsv(tx.getStatus())).append("\n");
        }

        return sb.toString();
    }

    /**
     * Defense in depth against spreadsheet formula injection (CSV Injection).
     * If a cell begins with '=', '+', '-', '@', prepend a single quote (').
     * Enclose values in double quotes if they contain commas, quotes, or newlines.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String clean = value.trim();

        // Formula injection defense: escape leading trigger characters
        if (!clean.isEmpty()) {
            char first = clean.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                clean = "'" + clean;
            }
        }

        // CSV quote escaping
        clean = clean.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }
}
