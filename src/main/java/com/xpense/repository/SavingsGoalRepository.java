package com.xpense.repository;

import com.xpense.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, String> {
    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(String userId);
}
