package com.xpense.service;

import com.xpense.dto.BillPaymentDTO;
import com.xpense.dto.BillRequest;
import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.RecurringBill;
import com.xpense.model.Transaction;
import com.xpense.model.Wallet;
import com.xpense.repository.RecurringBillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
public class RecurringBillService {

    private static final Set<String> FREQUENCIES = Set.of("weekly", "monthly", "yearly");

    private final RecurringBillRepository billRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final UserProfileService userProfileService;

    public RecurringBillService(RecurringBillRepository billRepository,
                                WalletService walletService,
                                TransactionService transactionService,
                                UserProfileService userProfileService) {
        this.billRepository = billRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.userProfileService = userProfileService;
    }

    public List<RecurringBill> getActiveBills(String userId) {
        return billRepository.findByUserIdAndIsActiveTrueOrderByNextDueDateAsc(userId);
    }

    public RecurringBill getOwnedBill(String id, String userId) {
        return billRepository.findByIdAndUserId(id, userId)
                .filter(b -> !Boolean.FALSE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found."));
    }

    @Transactional
    public RecurringBill createBill(String userId, BillRequest input) {
        RecurringBill bill = new RecurringBill();
        bill.setUserId(userId);
        bill.setIsActive(true);
        apply(bill, input, userId, true);
        return billRepository.save(bill);
    }

    @Transactional
    public RecurringBill updateBill(String id, String userId, BillRequest input) {
        RecurringBill bill = getOwnedBill(id, userId);
        apply(bill, input, userId, false);
        return billRepository.save(bill);
    }

    /** Bills are switched off rather than erased, so past payments still make sense. */
    @Transactional
    public void deleteBill(String id, String userId) {
        RecurringBill bill = getOwnedBill(id, userId);
        bill.setIsActive(false);
        billRepository.save(bill);
    }

    /**
     * Pays a bill as one unit: records the expense (from the bill's budget, with the usual
     * "enough money" checks), marks it paid today and moves the due date forward one period.
     * The user's money lock is taken first, so a double click can't pay twice with the same money.
     */
    @Transactional
    public BillPaymentDTO payBill(String id, String userId) {
        userProfileService.lockForMoneyChange(userId);
        RecurringBill bill = getOwnedBill(id, userId);

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setTitle(bill.getName());
        tx.setAmount(bill.getAmount());
        tx.setType("expense");
        tx.setPaymentMethod("Bill payment");
        tx.setNote("Bill payment");
        tx.setMerchant(bill.getName());
        if (bill.getWalletId() != null && walletService.ownsWallet(bill.getWalletId(), userId)) {
            Wallet wallet = walletService.getOwnedWallet(bill.getWalletId(), userId);
            tx.setWalletId(wallet.getId());
            tx.setCategory(wallet.getCategory());
        } else {
            tx.setCategory("Bills");
        }
        Transaction saved = transactionService.createTransaction(tx);

        bill.setLastPaidDate(LocalDate.now());
        bill.setNextDueDate(nextDate(bill.getNextDueDate(), bill.getFrequency(), bill.getDueDay()));
        RecurringBill updated = billRepository.save(bill);

        return new BillPaymentDTO(updated, saved);
    }

    /** Next due date one period later, keeping the original day of the month where the month allows it. */
    static LocalDate nextDate(LocalDate from, String frequency, Integer dueDay) {
        LocalDate base = from != null ? from : LocalDate.now();
        int day = dueDay != null ? dueDay : base.getDayOfMonth();
        String f = frequency == null ? "monthly" : frequency;
        if ("weekly".equals(f)) {
            return base.plusWeeks(1);
        }
        YearMonth next = YearMonth.from(base).plusMonths("yearly".equals(f) ? 12 : 1);
        return next.atDay(Math.min(day, next.lengthOfMonth()));
    }

    private void apply(RecurringBill bill, BillRequest input, String userId, boolean creating) {
        if (input.getName() != null || creating) {
            bill.setName(Inputs.requiredText(input.getName(), 100, "The bill name",
                    "Give the bill a name, like Rent or Electricity."));
        }

        if (input.getAmount() != null) {
            Inputs.requireAmount(input.getAmount());
            bill.setAmount(input.getAmount());
        } else if (creating) {
            throw new BadRequestException("Enter how much the bill is.");
        }

        if (input.getFrequency() != null) {
            String f = input.getFrequency().trim().toLowerCase();
            if (!FREQUENCIES.contains(f)) {
                throw new BadRequestException("How often must be weekly, monthly or yearly.");
            }
            bill.setFrequency(f);
        } else if (creating) {
            bill.setFrequency("monthly");
        }

        if (input.getNextDueDate() != null) {
            bill.setNextDueDate(input.getNextDueDate());
            bill.setDueDay(input.getNextDueDate().getDayOfMonth());
        } else if (creating) {
            throw new BadRequestException("Choose when the bill is next due.");
        }

        if (input.getWalletId() != null) {
            if (input.getWalletId().isBlank()) {
                bill.setWalletId(null);
            } else {
                bill.setWalletId(walletService.getOwnedWallet(input.getWalletId(), userId).getId());
            }
        }
    }
}
