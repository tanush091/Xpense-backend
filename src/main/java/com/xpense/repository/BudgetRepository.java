package com.xpense.repository;

import com.xpense.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    List<Budget> findByUserIdOrderByCategoryAsc(String userId);
    Optional<Budget> findByUserIdAndCategoryIgnoreCase(String userId, String category);
}
