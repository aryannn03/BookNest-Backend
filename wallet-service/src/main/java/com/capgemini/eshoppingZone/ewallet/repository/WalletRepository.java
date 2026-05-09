package com.capgemini.eshoppingZone.ewallet.repository;

import com.capgemini.eshoppingZone.ewallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    // Find wallet by userId
    Optional<Wallet> findByUserId(int userId);

    // Check if wallet exists for user
    boolean existsByUserId(int userId);

    // Get all wallets
    List<Wallet> findAll();

    // Delete wallet by userId
    void deleteByUserId(int userId);
}