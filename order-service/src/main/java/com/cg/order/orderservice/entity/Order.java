package com.cg.order.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    private int userId;

    private LocalDate orderDate;

    private double amountPaid;

    private String modeOfPayment;
    
    private String razorpayPaymentId;

    // Placed, Confirmed, Dispatched, Delivered, Cancelled
    private String orderStatus;

    private int quantity;

    // Book reference
    private int bookId;
    private String bookTitle;
    private double bookPrice;


    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDate.now();
        if (this.orderStatus == null) this.orderStatus = "Placed";
    }
}