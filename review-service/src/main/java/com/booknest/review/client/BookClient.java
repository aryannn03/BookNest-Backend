package com.booknest.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "book-service")
public interface BookClient {

    @PutMapping("/books/update-rating/{bookId}")
    void updateRating(@PathVariable int bookId,
                      @RequestParam double rating);
}