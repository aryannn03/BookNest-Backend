package com.cg.order.orderservice.client;

import com.cg.order.orderservice.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{bookId}")
    BookResponse getBookById(@PathVariable int bookId);

    @PutMapping("/books/update-stock/{bookId}")
    void updateStock(@PathVariable int bookId,
                     @RequestParam int quantity);
}