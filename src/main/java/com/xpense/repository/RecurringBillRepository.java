package com.xpense.repository;

import com.xpense.model.RecurringBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringBillRepository extends JpaRepository<RecurringBill, String> {
    List<RecurringBill> findByUserIdAndIsActiveTrueOrderByNextDueDateAsc(String userId);

    Optional<RecurringBill> findByIdAndUserId(String id, String userId);

    List<RecurringBill> findByUserIdAndWalletId(String userId, String walletId);
}
