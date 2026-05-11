package com.booknest.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @NotBlank
    private String fullName;


    @Column(unique = true, nullable = false)
    private String email;
    
    @ToString.Exclude
    private String passwordHash;

    // CUSTOMER or ADMIN
    private String role;

    // LOCAL or GITHUB
    private String provider;

    private Long mobile;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) this.role = "CUSTOMER";
        if (this.provider == null) this.provider = "LOCAL";
    }
}