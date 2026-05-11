package com.booknest.wishlist.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cart-service")
public interface CartClient {

    @PostMapping("/cart/add")
    Object addToCart(@RequestHeader("Authorization") String authHeader,
                     @RequestParam int bookId,
                     @RequestParam int quantity);
}