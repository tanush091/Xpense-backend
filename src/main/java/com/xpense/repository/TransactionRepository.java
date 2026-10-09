package com.xpense.repository;

import com.xpense.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    List<Transaction> findByUserIdOrderByDateDesc(String userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND (:category IS NULL OR LOWER(t.category) = LOWER(:category)) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:walletId IS NULL OR t.walletId = :walletId) " +
           "ORDER BY t.date DESC")
    List<Transaction> findFiltered(
            @Param("userId") String userId,
            @Param("category") String category,
            @Param("type") String type,
            @Param("walletId") String walletId
    );

    List<Transaction> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);
}
