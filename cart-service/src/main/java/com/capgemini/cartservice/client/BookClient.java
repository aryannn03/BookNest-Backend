package com.capgemini.cartservice.client;

import com.capgemini.cartservice.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{bookId}")
    BookResponse getBookById(@PathVariable int bookId);
}