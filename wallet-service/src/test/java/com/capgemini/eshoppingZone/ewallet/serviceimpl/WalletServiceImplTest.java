package com.capgemini.eshoppingZone.ewallet.serviceimpl;

import com.capgemini.eshoppingZone.ewallet.entity.Statement;
import com.capgemini.eshoppingZone.ewallet.entity.Wallet;
import com.capgemini.eshoppingZone.ewallet.repository.StatementRepository;
import com.capgemini.eshoppingZone.ewallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private StatementRepository statementRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet mockWallet;
    private Statement mockStatement;

    @BeforeEach
    void setUp() {
        mockWallet = new Wallet();
        mockWallet.setWalletId(1);
        mockWallet.setUserId(1);
        mockWallet.setCurrentBalance(1000.0);
        mockWallet.setStatements(new ArrayList<>());

        mockStatement = new Statement();
        mockStatement.setStatementId(1);
        mockStatement.setTransactionType("Deposit");
        mockStatement.setAmount(500.0);
        mockStatement.setOrderId(0);
        mockStatement.setTransactionRemarks("Wallet top-up");
        mockStatement.setWallet(mockWallet);
    }

    // ─── Add Wallet ───────────────────────────────────────────────────────────

    @Test
    void addWallet_Success() {
        when(walletRepository.existsByUserId(1)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        Wallet result = walletService.addWallet(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void addWallet_AlreadyExists_ThrowsException() {
        when(walletRepository.existsByUserId(1)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.addWallet(1));
        assertTrue(ex.getMessage().contains("Wallet already exists"));
    }

    // ─── Get All Wallets ──────────────────────────────────────────────────────

    @Test
    void getWallets_ReturnsList() {
        when(walletRepository.findAll()).thenReturn(List.of(mockWallet));

        List<Wallet> wallets = walletService.getWallets();

        assertEquals(1, wallets.size());
        assertEquals(1, wallets.get(0).getUserId());
    }

    // ─── Get By UserId ────────────────────────────────────────────────────────

    @Test
    void getById_WalletExists_ReturnsWallet() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));

        Wallet result = walletService.getById(1);

        assertNotNull(result);
        assertEquals(1000.0, result.getCurrentBalance());
    }

    @Test
    void getById_WalletNotFound_ThrowsException() {
        when(walletRepository.findByUserId(99))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.getById(99));
        assertTrue(ex.getMessage().contains("Wallet not found"));
    }

    // ─── Find Or Create Wallet ────────────────────────────────────────────────

    @Test
    void findOrCreateWallet_Exists_ReturnsExisting() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));

        Wallet result = walletService.findOrCreateWallet(1);

        assertNotNull(result);
        assertEquals(1000.0, result.getCurrentBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void findOrCreateWallet_NotExists_CreatesNew() {
        Wallet newWallet = new Wallet();
        newWallet.setUserId(1);
        newWallet.setCurrentBalance(0.0);

        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        Wallet result = walletService.findOrCreateWallet(1);

        assertNotNull(result);
        assertEquals(0.0, result.getCurrentBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    // ─── Add Money ────────────────────────────────────────────────────────────

    @Test
    void addMoney_Success() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        Wallet result = walletService.addMoney(1, 500.0, "Top-up");

        assertNotNull(result);
        assertEquals(1500.0, mockWallet.getCurrentBalance());
        assertEquals(1, mockWallet.getStatements().size());
        assertEquals("Deposit", mockWallet.getStatements()
                .get(0).getTransactionType());
    }

    @Test
    void addMoney_ZeroAmount_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.addMoney(1, 0.0, "Top-up"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void addMoney_NegativeAmount_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.addMoney(1, -100.0, "Top-up"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void addMoney_NullRemarks_UsesDefault() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        Wallet result = walletService.addMoney(1, 500.0, null);

        assertNotNull(result);
        assertEquals("Wallet top-up",
                mockWallet.getStatements().get(0).getTransactionRemarks());
    }

    // ─── Pay Money ────────────────────────────────────────────────────────────

    @Test
    void payMoney_Success() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        Wallet result = walletService.payMoney(1, 500.0, 101);

        assertNotNull(result);
        assertEquals(500.0, mockWallet.getCurrentBalance());
        assertEquals(1, mockWallet.getStatements().size());
        assertEquals("Withdraw", mockWallet.getStatements()
                .get(0).getTransactionType());
        assertEquals(101, mockWallet.getStatements()
                .get(0).getOrderId());
    }

    @Test
    void payMoney_InsufficientBalance_ThrowsException() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.payMoney(1, 2000.0, 101));
        assertTrue(ex.getMessage().contains("Insufficient wallet balance"));
    }

    @Test
    void payMoney_ZeroAmount_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.payMoney(1, 0.0, 101));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void payMoney_NegativeAmount_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.payMoney(1, -100.0, 101));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    // ─── Get Statements ───────────────────────────────────────────────────────

    @Test
    void getStatementsById_ReturnsStatements() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));
        when(statementRepository.findByWalletWalletId(1))
                .thenReturn(List.of(mockStatement));

        List<Statement> statements = walletService.getStatementsById(1);

        assertEquals(1, statements.size());
        assertEquals("Deposit", statements.get(0).getTransactionType());
    }

    @Test
    void getStatements_ReturnsAllStatements() {
        when(statementRepository.findAll())
                .thenReturn(List.of(mockStatement));

        List<Statement> statements = walletService.getStatements();

        assertEquals(1, statements.size());
    }

    // ─── Delete Wallet ────────────────────────────────────────────────────────

    @Test
    void deleteById_Success() {
        when(walletRepository.findByUserId(1))
                .thenReturn(Optional.of(mockWallet));

        assertDoesNotThrow(() -> walletService.deleteById(1));
        verify(walletRepository).delete(mockWallet);
    }

    @Test
    void deleteById_WalletNotFound_ThrowsException() {
        when(walletRepository.findByUserId(99))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> walletService.deleteById(99));
    }
}