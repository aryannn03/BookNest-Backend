package com.cg.order.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/send")
    void sendNotification(@RequestParam int userId,
                          @RequestParam String type,
                          @RequestParam String message);
}