package com.booknest.book.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookId;
    
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "ISBN is required")
    @Column(unique = true)
    private String isbn;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Double price;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;


    private String genre;

    private String publisher;

    private double rating;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String coverImageUrl;

    private LocalDate publishedDate;

    // featured flag for homepage display
    private boolean featured;

    @PrePersist
    public void prePersist() {
        if (this.rating == 0) this.rating = 0.0;
        if (this.stock < 0) this.stock = 0;
    }
}