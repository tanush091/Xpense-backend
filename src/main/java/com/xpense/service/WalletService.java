package com.xpense.service;

import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.Wallet;
import com.xpense.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserProfileService userProfileService;

    public WalletService(WalletRepository walletRepository, UserProfileService userProfileService) {
        this.walletRepository = walletRepository;
        this.userProfileService = userProfileService;
    }

    public List<Wallet> getWalletsByUserId(String userId) {
        return walletRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    public Wallet getWalletById(String id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + id));
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

    @Transactional
    public Wallet createWallet(Wallet wallet) {
        if (wallet.getUserId() == null || wallet.getUserId().isEmpty()) {
            wallet.setUserId(UserProfileService.DEFAULT_USER_ID);
        }
        if (wallet.getBalance() == null) {
            wallet.setBalance(BigDecimal.ZERO);
        }
        if (wallet.getBudgetLimit() == null) {
            wallet.setBudgetLimit(new BigDecimal("1000.00"));
        }
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet updateWallet(String id, Wallet updateData) {
        Wallet wallet = getWalletById(id);
        if (updateData.getName() != null) wallet.setName(updateData.getName());
        if (updateData.getCategory() != null) wallet.setCategory(updateData.getCategory());
        if (updateData.getBalance() != null) wallet.setBalance(updateData.getBalance());
        if (updateData.getBudgetLimit() != null) wallet.setBudgetLimit(updateData.getBudgetLimit());
        if (updateData.getIcon() != null) wallet.setIcon(updateData.getIcon());
        if (updateData.getColor() != null) wallet.setColor(updateData.getColor());
        
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        return walletRepository.save(wallet);
    }

    @Transactional
    public void deleteWallet(String id) {
        Wallet wallet = getWalletById(id);
        walletRepository.delete(wallet);
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

    @Transactional
    public Wallet topUpWallet(String id, BigDecimal amount) {
        return topUpWallet(id, null, amount, false);
    }

    /**
     * Adds money to a wallet owned by {@code userId}.
     * fromAvailable = true moves unbudgeted money into the wallet (total balance unchanged);
     * fromAvailable = false records new money arriving straight into the wallet (total balance grows).
     */
    @Transactional
    public Wallet topUpWallet(String id, String userId, BigDecimal amount, boolean fromAvailable) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Top-up amount must be greater than zero");
        }
        Wallet wallet = getWalletById(id);
        if (userId != null && !userId.equals(wallet.getUserId())) {
            throw new ResourceNotFoundException("Wallet not found with id: " + id);
        }

        if (fromAvailable) {
            BigDecimal unbudgeted = getUnbudgetedBalance(wallet.getUserId());
            if (unbudgeted.compareTo(amount) < 0) {
                throw new BadRequestException("You only have ₹" + unbudgeted.toPlainString()
                        + " that is not in a budget yet. Add money first or choose a smaller amount.");
            }
        }

        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(current.add(amount));
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));

        Wallet saved = walletRepository.save(wallet);

        if (!fromAvailable) {
            // New money arriving directly into this wallet also grows the total balance
            userProfileService.adjustBalance(wallet.getUserId(), amount);
        }

        return saved;
    }

    @Transactional
    public Wallet refundToWallet(String id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        Wallet wallet = getWalletById(id);
        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(current.add(amount));
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet deductFromWallet(String id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Deduct amount must be greater than zero");
        }
        Wallet wallet = getWalletById(id);
        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance in wallet: " + wallet.getName());
        }
        wallet.setBalance(current.subtract(amount));
        wallet.setStatus(calculateHealthStatus(wallet.getBalance(), wallet.getBudgetLimit()));

        return walletRepository.save(wallet);
    }
}
