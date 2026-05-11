package com.booknest.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviewId;

    private int bookId;

    private int userId;
    
    private String fullName;

    // Rating 1-5 stars
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDate reviewDate;

    // Only verified purchasers can review
    private boolean verified;

    @PrePersist
    public void prePersist() {
        this.reviewDate = LocalDate.now();
    }
}