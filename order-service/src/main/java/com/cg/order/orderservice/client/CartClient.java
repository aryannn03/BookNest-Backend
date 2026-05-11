package com.cg.order.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/cart/user/{userId}")
    Map<String, Object> getCartByUserId(@PathVariable int userId);

    @DeleteMapping("/cart/user/{userId}/clear")
    void clearCart(@PathVariable int userId);
}