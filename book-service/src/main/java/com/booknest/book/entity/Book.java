package com.booknest.book.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookId;

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String isbn;

    @NotBlank
    private String genre;

    private String publisher;

    @NotNull
    @Positive
    private double price;

    @NotNull
    private int stock;

    private double rating;

    @Column(length = 2000)
    private String description;

    private String coverImageUrl;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private boolean featured = false;

    @PrePersist
    protected void onCreate() {
        if (this.rating == 0) {
            this.rating = 0.0;
        }
    }
}