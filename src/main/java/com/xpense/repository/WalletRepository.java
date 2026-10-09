package com.xpense.repository;

import com.xpense.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUserIdOrderByCreatedAtAsc(String userId);
}
