package com.booknest.review.client;

import com.booknest.review.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/orders/my-orders-by-user/{userId}")
    OrderResponse[] getOrdersByUserId(@PathVariable int userId);
}