package com.capgemini.eshoppingZone.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int walletId;

    // Links wallet to a specific user
    @Column(unique = true)
    private int userId;

    private double currentBalance;

    // One wallet has many statements
    @OneToMany(mappedBy = "wallet",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<Statement> statements = new ArrayList<>();

}