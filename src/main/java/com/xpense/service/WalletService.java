package com.xpense.service;

import com.xpense.dto.WalletUpdateRequest;
import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.Transaction;
import com.xpense.model.Wallet;
import com.xpense.repository.RecurringBillRepository;
import com.xpense.repository.TransactionRepository;
import com.xpense.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserProfileService userProfileService;
    private final TransactionRepository transactionRepository;
    private final RecurringBillRepository billRepository;

    public WalletService(WalletRepository walletRepository,
                         UserProfileService userProfileService,
                         TransactionRepository transactionRepository,
                         RecurringBillRepository billRepository) {
        this.walletRepository = walletRepository;
        this.userProfileService = userProfileService;
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
    }

    public List<Wallet> getWalletsByUserId(String userId) {
        return walletRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /** Loads a wallet only if it belongs to {@code userId}; otherwise behaves as if it doesn't exist. */
    public Wallet getOwnedWallet(String id, String userId) {
        return walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found."));
    }

    public boolean ownsWallet(String id, String userId) {
        return id != null && walletRepository.findByIdAndUserId(id, userId).isPresent();
    }

    public String calculateHealthStatus(BigDecimal balance, BigDecimal budgetLimit) {
        if (budgetLimit == null || budgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) ? "Good" : "Warning";
        }
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return "Warning";
        }
        double ratio = balance.doubleValue() / budgetLimit.doubleValue();
        if (ratio >= 0.30) {
            return "Good";
        } else if (ratio >= 0.10) {
            return "Low";
        } else {
            return "Warning";
        }
    }

    /**
     * Money the user has recorded but not yet placed in any wallet:
     * total balance minus the sum of all wallet balances (never negative).
     */
    public BigDecimal getUnbudgetedBalance(String userId) {
        BigDecimal total = userProfileService.getProfile(userId).getTotalBalance();
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        BigDecimal inWallets = walletRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unbudgeted = total.subtract(inWallets);
        return unbudgeted.compareTo(BigDecimal.ZERO) > 0 ? unbudgeted : BigDecimal.ZERO;
    }

    /** New wallets always start empty; money goes in through top-up so the totals stay correct. */
    @Transactional
    public Wallet createWallet(Wallet wallet) {
        wallet.setName(Inputs.requiredText(wallet.getName(), 80, "The budget name", "Give this budget a name."));
        String category = Inputs.text(wallet.getCategory(), 64, "The category");
        wallet.setCategory(category == null || category.isEmpty() ? "Other" : category);
        wallet.setId(null);
        wallet.setBalance(BigDecimal.ZERO);
        if (wallet.getBudgetLimit() == null) {
            wallet.setBudgetLimit(new BigDecimal("1000.00"));
        } else {
            Inputs.requireAmount(wallet.getBudgetLimit());
        }
        boolean isTax = Boolean.TRUE.equals(wallet.getIsTaxReserve());
        wallet.setIsTaxReserve(isTax);
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        Wallet saved = walletRepository.save(wallet);
        if (isTax) {
            clearOtherTaxReserves(saved.getUserId(), saved.getId());
        }
        return saved;
    }

    /** Edits only the fields that were sent. The balance only changes through money actions. */
    @Transactional
    public Wallet updateWallet(String id, String userId, WalletUpdateRequest update) {
        Wallet wallet = getOwnedWallet(id, userId);
        if (update.getName() != null) {
            wallet.setName(Inputs.requiredText(update.getName(), 80, "The budget name", "The name can't be empty."));
        }
        if (update.getCategory() != null && !update.getCategory().isBlank()) {
            wallet.setCategory(Inputs.text(update.getCategory(), 64, "The category"));
        }
        if (update.getBudgetLimit() != null) {
            Inputs.requireAmount(update.getBudgetLimit());
            wallet.setBudgetLimit(update.getBudgetLimit());
        }
        if (update.getIcon() != null) wallet.setIcon(Inputs.text(update.getIcon(), 64, "The icon"));
        if (update.getColor() != null) wallet.setColor(Inputs.text(update.getColor(), 32, "The colour"));
        if (update.getIsTaxReserve() != null) {
            wallet.setIsTaxReserve(update.getIsTaxReserve());
            if (update.getIsTaxReserve()) {
                clearOtherTaxReserves(userId, wallet.getId());
            }
        }

        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        return walletRepository.save(wallet);
    }

    /**
     * Deleting a wallet keeps the total; its money becomes "not in a budget yet", and any bills
     * paid from it are switched to "money not in a budget" (as the app shows them).
     */
    @Transactional
    public void deleteWallet(String id, String userId) {
        userProfileService.lockForMoneyChange(userId);
        Wallet wallet = getOwnedWallet(id, userId);
        billRepository.findByUserIdAndWalletId(userId, wallet.getId()).forEach(bill -> {
            bill.setWalletId(null);
            billRepository.save(bill);
        });
        walletRepository.delete(wallet);
    }

    /**
     * Adds money to a wallet owned by {@code userId}.
     * fromAvailable = true moves unbudgeted money into the wallet (total balance unchanged);
     * fromAvailable = false records new money arriving straight into the wallet (total grows,
     * and a "money in" entry is added to the history so it always adds up).
     */
    @Transactional
    public Wallet topUpWallet(String id, String userId, BigDecimal amount, boolean fromAvailable) {
        userProfileService.lockForMoneyChange(userId);
        Inputs.requireAmount(amount);
        Wallet wallet = getOwnedWallet(id, userId);

        if (fromAvailable) {
            BigDecimal unbudgeted = getUnbudgetedBalance(userId);
            if (unbudgeted.compareTo(amount) < 0) {
                throw new BadRequestException("You only have " + Inputs.inr(unbudgeted)
                        + " that is not in a budget yet. Add money first or choose a smaller amount.");
            }
        }

        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(current.add(amount));
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        Wallet saved = walletRepository.save(wallet);

        if (!fromAvailable) {
            userProfileService.adjustBalance(userId, amount);
            Transaction moneyIn = new Transaction();
            moneyIn.setUserId(userId);
            moneyIn.setWalletId(saved.getId());
            moneyIn.setWalletName(saved.getName());
            moneyIn.setTitle("Money added to " + saved.getName());
            moneyIn.setAmount(amount);
            moneyIn.setType("income");
            moneyIn.setCategory("Income");
            moneyIn.setPaymentMethod("Added to budget");
            moneyIn.setStatus("completed");
            moneyIn.setDate(LocalDateTime.now());
            transactionRepository.save(moneyIn);
        }

        return saved;
    }

    /** Puts money back into a wallet when an expense is deleted. Missing wallets are ignored. */
    @Transactional
    public void refundToWallet(String id, String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        walletRepository.findByIdAndUserId(id, userId).ifPresent(wallet -> {
            BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
            wallet.setBalance(current.add(amount));
            wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
            walletRepository.save(wallet);
        });
    }

    /** Takes money out of a wallet. Callers must already hold the user's money lock. */
    @Transactional
    public Wallet deductFromWallet(String id, String userId, BigDecimal amount) {
        Inputs.requireAmount(amount);
        Wallet wallet = getOwnedWallet(id, userId);
        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new BadRequestException(wallet.getName() + " only has " + Inputs.inr(current)
                    + " left. Add money to it first or pick another budget.");
        }
        wallet.setBalance(current.subtract(amount));
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));

        return walletRepository.save(wallet);
    }

    /** Only one budget per user can be the tax budget. */
    private void clearOtherTaxReserves(String userId, String keepId) {
        walletRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsTaxReserve()) && !w.getId().equals(keepId))
                .forEach(w -> {
                    w.setIsTaxReserve(false);
                    walletRepository.save(w);
                });
    }
}
