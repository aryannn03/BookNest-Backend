package com.capgemini.eshoppingZone.ewallet.repository;

import com.capgemini.eshoppingZone.ewallet.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Integer> {

    // Find statements by walletId
    List<Statement> findByWalletWalletId(int walletId);

    // Find by transaction type
    List<Statement> findByTransactionType(String transactionType);

    // Find by orderId
    Optional<Statement> findByOrderId(int orderId);
}