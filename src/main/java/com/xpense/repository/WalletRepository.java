package com.xpense.repository;

import com.xpense.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<Wallet> findByIdAndUserId(String id, String userId);
}
