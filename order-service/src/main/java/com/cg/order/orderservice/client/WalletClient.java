package com.cg.order.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @GetMapping("/wallet/{userId}")
    Map<String, Object> getWallet(@PathVariable int userId);

    @PutMapping("/wallet/pay/{userId}")
    void payMoney(@PathVariable int userId,
                  @RequestParam double amount,
                  @RequestParam int orderId);

    @PutMapping("/wallet/refund/{userId}")
    void refundMoney(@PathVariable int userId,
                     @RequestParam double amount);
}