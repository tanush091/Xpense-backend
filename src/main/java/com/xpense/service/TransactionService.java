package com.xpense.service;

import com.xpense.dto.TransferRequest;
import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.Transaction;
import com.xpense.model.Wallet;
import com.xpense.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final UserProfileService userProfileService;
    private final BudgetService budgetService;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletService walletService,
                              UserProfileService userProfileService,
                              BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.userProfileService = userProfileService;
        this.budgetService = budgetService;
    }

    public List<Transaction> getTransactions(String userId, String category, String type, String walletId) {
        if (category == null && type == null && walletId == null) {
            return transactionRepository.findByUserIdOrderByDateDesc(userId);
        }
        return transactionRepository.findFiltered(userId, category, type, walletId);
    }

    public Transaction getTransactionById(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getUserId() == null || transaction.getUserId().isEmpty()) {
            transaction.setUserId(UserProfileService.DEFAULT_USER_ID);
        }

        BigDecimal amount = transaction.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Please enter a valid transaction amount");
        }

        // 1. If linked to a wallet and is an expense, deduct from wallet
        if (transaction.getWalletId() != null && "expense".equalsIgnoreCase(transaction.getType())) {
            Wallet wallet = walletService.deductFromWallet(transaction.getWalletId(), amount);
            if (transaction.getWalletName() == null) {
                transaction.setWalletName(wallet.getName());
            }
        }

        // 2. Adjust total balance on user profile
        BigDecimal balanceChange = "income".equalsIgnoreCase(transaction.getType()) ? amount : amount.negate();
        userProfileService.adjustBalance(transaction.getUserId(), balanceChange);

        // 3. Update budget if it's an expense
        if ("expense".equalsIgnoreCase(transaction.getType()) && transaction.getCategory() != null) {
            budgetService.recordExpense(transaction.getUserId(), transaction.getCategory(), amount);
        }

        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction processTransfer(String userId, TransferRequest transferRequest) {
        if (userId == null || userId.isEmpty()) {
            userId = UserProfileService.DEFAULT_USER_ID;
        }

        BigDecimal amount = transferRequest.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid transfer amount");
        }

        String walletName = "Main Account";
        if (transferRequest.getWalletId() != null && !transferRequest.getWalletId().isEmpty()) {
            Wallet wallet = walletService.deductFromWallet(transferRequest.getWalletId(), amount);
            walletName = wallet.getName();
        }

        // Adjust user balance
        userProfileService.adjustBalance(userId, amount.negate());

        // Create transaction record
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setWalletId(transferRequest.getWalletId());
        tx.setWalletName(walletName);
        tx.setTitle("Transfer to " + transferRequest.getRecipient());
        tx.setAmount(amount);
        tx.setType("expense");
        tx.setCategory(transferRequest.getCategory() != null ? transferRequest.getCategory() : "Transfers");
        tx.setRecipient(transferRequest.getRecipient());
        tx.setPaymentMethod(transferRequest.getPaymentMethod() != null ? transferRequest.getPaymentMethod() : "UPI");
        tx.setStatus("completed");
        tx.setNote(transferRequest.getNote());
        tx.setDate(LocalDateTime.now());

        return transactionRepository.save(tx);
    }

    @Transactional
    public void deleteTransaction(String id) {
        Transaction tx = getTransactionById(id);
        BigDecimal amount = tx.getAmount();

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if ("income".equalsIgnoreCase(tx.getType())) {
                // Revert income by debiting user total balance
                userProfileService.adjustBalance(tx.getUserId(), amount.negate());
            } else if ("expense".equalsIgnoreCase(tx.getType())) {
                // Revert expense by crediting user total balance
                userProfileService.adjustBalance(tx.getUserId(), amount);

                // Refund wallet if one was linked
                if (tx.getWalletId() != null && !tx.getWalletId().isEmpty()) {
                    walletService.refundToWallet(tx.getWalletId(), amount);
                }

                // Revert budget spent amount if category exists
                if (tx.getCategory() != null && !tx.getCategory().isEmpty()) {
                    budgetService.revertExpense(tx.getUserId(), tx.getCategory(), amount);
                }
            }
        }

        transactionRepository.delete(tx);
    }
}
