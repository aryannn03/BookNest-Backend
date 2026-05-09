package com.capgemini.eshoppingZone.ewallet.serviceimpl;

import com.capgemini.eshoppingZone.ewallet.entity.Statement;
import com.capgemini.eshoppingZone.ewallet.entity.Wallet;
import com.capgemini.eshoppingZone.ewallet.repository.StatementRepository;
import com.capgemini.eshoppingZone.ewallet.repository.WalletRepository;
import com.capgemini.eshoppingZone.ewallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private StatementRepository statementRepository;

    // ─── Add Wallet ───────────────────────────────────────────────────────────

    @Override
    public Wallet addWallet(int userId) {
        // Check if wallet already exists
        if (walletRepository.existsByUserId(userId)) {
            throw new RuntimeException(
                    "Wallet already exists for user: " + userId);
        }
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setCurrentBalance(0.0);
        return walletRepository.save(wallet);
    }

    // ─── Get All Wallets ──────────────────────────────────────────────────────

    @Override
    public List<Wallet> getWallets() {
        return walletRepository.findAll();
    }

    // ─── Get Wallet By UserId ─────────────────────────────────────────────────

    @Override
    public Wallet getById(int userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Wallet not found for user: " + userId));
    }

    // ─── Add Money (Deposit) ──────────────────────────────────────────────────

    @Override
    @Transactional
    public Wallet addMoney(int userId, double amount, String remarks) {
        if (amount <= 0) {
            throw new RuntimeException(
                    "Deposit amount must be greater than zero");
        }

        Wallet wallet = getById(userId);

        // Update balance
        wallet.setCurrentBalance(wallet.getCurrentBalance() + amount);

        // Create statement record
        Statement statement = new Statement();
        statement.setTransactionType("Deposit");
        statement.setAmount(amount);
        statement.setTransactionRemarks(
                remarks != null ? remarks : "Wallet top-up");
        statement.setOrderId(0);
        statement.setWallet(wallet);

        wallet.getStatements().add(statement);

        return walletRepository.save(wallet);
    }

    // ─── Pay Money (Withdraw) ─────────────────────────────────────────────────

    @Override
    @Transactional
    public Wallet payMoney(int userId, double amount, int orderId) {
        if (amount <= 0) {
            throw new RuntimeException(
                    "Payment amount must be greater than zero");
        }

        Wallet wallet = getById(userId);

        // Validate balance — ACID compliance, no double spend
        if (wallet.getCurrentBalance() < amount) {
            throw new RuntimeException(
                    "Insufficient wallet balance. "
                    + "Required: " + amount
                    + " Available: " + wallet.getCurrentBalance());
        }

        // Deduct balance
        wallet.setCurrentBalance(wallet.getCurrentBalance() - amount);

        // Create statement record
        Statement statement = new Statement();
        statement.setTransactionType("Withdraw");
        statement.setAmount(amount);
        statement.setTransactionRemarks(
                "Payment for order #" + orderId);
        statement.setOrderId(orderId);
        statement.setWallet(wallet);

        wallet.getStatements().add(statement);

        return walletRepository.save(wallet);
    }

    // ─── Get Statements By UserId ─────────────────────────────────────────────

    @Override
    public List<Statement> getStatementsById(int userId) {
        Wallet wallet = getById(userId);
        return statementRepository
                .findByWalletWalletId(wallet.getWalletId());
    }

    // ─── Get All Statements ───────────────────────────────────────────────────

    @Override
    public List<Statement> getStatements() {
        return statementRepository.findAll();
    }

    // ─── Delete Wallet ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteById(int userId) {
        Wallet wallet = getById(userId);
        walletRepository.delete(wallet);
    }
    
    @Override
    public Wallet findOrCreateWallet(int userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setCurrentBalance(0.0);
                    return walletRepository.save(wallet);
                });
    }
}