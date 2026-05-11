package com.cg.order.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int addressId;

    private int customerId;

    private String fullName;

    private String mobileNumber;

    private String flatNumber;
    
    private String street;

    private String city;

    private String pincode;

    private String state;
}