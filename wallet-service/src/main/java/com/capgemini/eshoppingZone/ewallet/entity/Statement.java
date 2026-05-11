package com.capgemini.eshoppingZone.ewallet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int statementId;

    // Deposit or Withdraw
    private String transactionType;

    private double amount;

    private LocalDateTime dateTime;

    private int orderId;

    private String transactionRemarks;

    // Many statements belong to one wallet
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    @JsonIgnore
    @ToString.Exclude 
    private Wallet wallet;

    @PrePersist
    public void prePersist() {
        this.dateTime = LocalDateTime.now();
    }
}