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
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<String> TYPES = Set.of("income", "expense", "transfer");

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

    public Transaction getOwnedTransaction(String id, String userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));
    }

    /**
     * Records money in or money out for {@code transaction.userId} as one atomic unit:
     * wallet balance, total balance, category budget and the ledger row change together.
     * The user's money lock is taken first, so simultaneous requests can't both pass the checks.
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        String userId = transaction.getUserId();
        userProfileService.lockForMoneyChange(userId);

        BigDecimal amount = transaction.getAmount();
        Inputs.requireAmount(amount);
        String type = transaction.getType() == null ? "" : transaction.getType().toLowerCase();
        if (!TYPES.contains(type)) {
            throw new BadRequestException("Type must be income or expense.");
        }
        transaction.setType(type);
        boolean isIncome = "income".equals(type);

        String title = Inputs.text(transaction.getTitle(), 120, "The description");
        transaction.setTitle(title == null || title.isEmpty() ? (isIncome ? "Money in" : "Expense") : title);
        transaction.setCategory(Inputs.text(transaction.getCategory(), 64, "The category"));
        transaction.setMerchant(Inputs.text(transaction.getMerchant(), 120, "The shop name"));
        transaction.setRecipient(Inputs.text(transaction.getRecipient(), 120, "The person's name"));
        transaction.setNote(Inputs.text(transaction.getNote(), 500, "The note"));
        transaction.setPaymentMethod(Inputs.text(transaction.getPaymentMethod(), 50, "The payment method"));

        boolean hasWallet = transaction.getWalletId() != null && !transaction.getWalletId().isBlank();

        if (isIncome) {
            // Money in always lands in "not in a budget yet"
            transaction.setWalletId(null);
            transaction.setWalletName(null);
        } else if (hasWallet) {
            Wallet wallet = walletService.deductFromWallet(transaction.getWalletId(), userId, amount);
            transaction.setWalletName(wallet.getName());
        } else {
            transaction.setWalletId(null);
            BigDecimal unbudgeted = walletService.getUnbudgetedBalance(userId);
            if (unbudgeted.compareTo(amount) < 0) {
                throw new BadRequestException("Choose a budget to pay from. You only have "
                        + Inputs.inr(unbudgeted) + " that is not in a budget.");
            }
            transaction.setWalletName("Not in a budget");
        }

        userProfileService.adjustBalance(userId, isIncome ? amount : amount.negate());

        if (!isIncome && transaction.getCategory() != null) {
            budgetService.recordExpense(userId, transaction.getCategory(), amount);
        }

        transaction.setId(null);
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }
        if (transaction.getStatus() == null) {
            transaction.setStatus("completed");
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction processTransfer(String userId, TransferRequest transferRequest) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setWalletId(transferRequest.getWalletId());
        tx.setTitle("Sent to " + Inputs.text(transferRequest.getRecipient(), 100, "The person's name"));
        tx.setAmount(transferRequest.getAmount());
        tx.setType("expense");
        tx.setCategory(transferRequest.getCategory() != null ? transferRequest.getCategory() : "Transfers");
        tx.setRecipient(transferRequest.getRecipient());
        tx.setPaymentMethod(transferRequest.getPaymentMethod() != null ? transferRequest.getPaymentMethod() : "UPI");
        tx.setNote(transferRequest.getNote());
        return createTransaction(tx);
    }

    /** Reverses a transaction's effect on balances, then removes it. */
    @Transactional
    public void deleteTransaction(String id, String userId) {
        userProfileService.lockForMoneyChange(userId);
        Transaction tx = getOwnedTransaction(id, userId);
        BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
        boolean hasWallet = tx.getWalletId() != null && !tx.getWalletId().isEmpty();

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if ("income".equalsIgnoreCase(tx.getType())) {
                if (hasWallet && walletService.ownsWallet(tx.getWalletId(), userId)) {
                    // New money that went straight into a budget comes back out of that budget
                    var wallet = walletService.getOwnedWallet(tx.getWalletId(), userId);
                    BigDecimal left = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
                    if (left.compareTo(amount) < 0) {
                        throw new BadRequestException("Some of this money has already been spent from " + wallet.getName()
                                + ", so it can't be removed. " + wallet.getName() + " has " + Inputs.inr(left) + " left.");
                    }
                    walletService.deductFromWallet(tx.getWalletId(), userId, amount);
                } else {
                    BigDecimal unbudgeted = walletService.getUnbudgetedBalance(userId);
                    if (unbudgeted.compareTo(amount) < 0) {
                        throw new BadRequestException("Some of this money is already in your budgets, savings or spent. "
                                + "You can only remove " + Inputs.inr(unbudgeted) + " right now.");
                    }
                }
                userProfileService.adjustBalance(userId, amount.negate());
            } else {
                userProfileService.adjustBalance(userId, amount);
                if (hasWallet) {
                    walletService.refundToWallet(tx.getWalletId(), userId, amount);
                }
                if (tx.getCategory() != null && !tx.getCategory().isEmpty()) {
                    budgetService.revertExpense(userId, tx.getCategory(), amount);
                }
            }
        }

        transactionRepository.delete(tx);
    }
}
