package com.capgemini.eshoppingZone.ewallet.resource;

import com.capgemini.eshoppingZone.ewallet.entity.Statement;
import com.capgemini.eshoppingZone.ewallet.entity.Wallet;
import com.capgemini.eshoppingZone.ewallet.security.JwtUtil;
import com.capgemini.eshoppingZone.ewallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletResource {

    @Autowired
    private WalletService walletService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── Helper — Extract userId from token ───────────────────────────────────

    private int extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException(
                    "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    // ─── Create Wallet ────────────────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<Wallet> createWallet(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        Wallet wallet = walletService.addWallet(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    // ─── Get My Wallet ────────────────────────────────────────────────────────

    @GetMapping("/my-wallet")
    public ResponseEntity<Wallet> getMyWallet(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(walletService.findOrCreateWallet(userId));
    }

    // ─── Get Wallet By UserId (internal service call) ─────────────────────────

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWalletByUserId(
            @PathVariable int userId) {
        return ResponseEntity.ok(walletService.getById(userId));
    }

    // ─── Add Money to Wallet ──────────────────────────────────────────────────

    @PutMapping("/add-money")
    public ResponseEntity<Wallet> addMoney(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam double amount,
            @RequestParam(required = false,
                    defaultValue = "Wallet top-up") String remarks) {
        int userId = extractUserId(authHeader);
        Wallet wallet = walletService.addMoney(userId, amount, remarks);
        return ResponseEntity.ok(wallet);
    }

    // ─── Pay Money (internal call from order-service) ─────────────────────────

    @PutMapping("/pay/{userId}")
    public ResponseEntity<Wallet> payMoney(
            @PathVariable int userId,
            @RequestParam double amount,
            @RequestParam int orderId) {
        Wallet wallet = walletService.payMoney(userId, amount, orderId);
        return ResponseEntity.ok(wallet);
    }

    // ─── Get My Statements ────────────────────────────────────────────────────

    @GetMapping("/my-statements")
    public ResponseEntity<List<Statement>> getMyStatements(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                walletService.getStatementsById(userId));
    }

    // ─── Get All Wallets (Admin) ──────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Wallet>> getAllWallets() {
        return ResponseEntity.ok(walletService.getWallets());
    }

    // ─── Get All Statements (Admin) ───────────────────────────────────────────

    @GetMapping("/all-statements")
    public ResponseEntity<List<Statement>> getAllStatements() {
        return ResponseEntity.ok(walletService.getStatements());
    }

    // ─── Delete Wallet ────────────────────────────────────────────────────────

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteWallet(
            @RequestHeader("Authorization") String authHeader) {
        int userId = extractUserId(authHeader);
        walletService.deleteById(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Wallet deleted successfully"));
    }
    @PutMapping("/refund/{userId}")
    public ResponseEntity<Wallet> refundMoney(
            @PathVariable int userId,
            @RequestParam double amount) {
        Wallet wallet = walletService.addMoney(userId, amount, "Refund for cancelled order");
        return ResponseEntity.ok(wallet);
    }

}