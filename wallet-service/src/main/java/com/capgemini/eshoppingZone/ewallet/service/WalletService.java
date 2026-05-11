package com.capgemini.eshoppingZone.ewallet.service;

import com.capgemini.eshoppingZone.ewallet.entity.Statement;
import com.capgemini.eshoppingZone.ewallet.entity.Wallet;

import java.util.List;

public interface WalletService {

    // Create new wallet for user
    Wallet addWallet(int userId);

    // Get all wallets (Admin)
    List<Wallet> getWallets();

    // Get wallet by userId
    Wallet getById(int userId);
    
    Wallet findOrCreateWallet(int userId);
    
    // Add money to wallet (Deposit)
    Wallet addMoney(int userId, double amount, String remarks);

    // Deduct money from wallet (Withdraw)
    Wallet payMoney(int userId, double amount, int orderId);

    // Get all statements for a wallet
    List<Statement> getStatementsById(int userId);

    // Get all statements (Admin)
    List<Statement> getStatements();

    // Delete wallet by userId
    void deleteById(int userId);
}