package com.cg.order.orderservice.resource;

import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;
import com.cg.order.orderservice.security.JwtUtil;
import com.cg.order.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderResource {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── Helper — Extract userId from token ───────────────────────────

    private int extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException(
                    "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    // ─── Helper — Extract role from token ────────────────────────────

    private String extractRole(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractRole(token);
    }

    // ─── Helper — Require ADMIN role ─────────────────────────────────

    private void requireAdmin(String authHeader) {
        extractUserId(authHeader); // validates token first
        String role = extractRole(authHeader);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access denied: Admin only");
        }
    }

    // ─── Place COD Order ──────────────────────────────────────────────
    // bookId & quantity removed — now fetched from cart on the backend

    @PostMapping("/place-cod")
    public ResponseEntity<List<Order>> placeOrderCOD(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam int addressId) {

        int userId = extractUserId(authHeader);
        List<Order> orders = orderService.placeOrder(userId, addressId, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orders);
    }

    // ─── Place Online (Wallet) Order ──────────────────────────────────

    @PostMapping("/place-online")
    public ResponseEntity<List<Order>> placeOrderOnline(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam int addressId) {

        int userId = extractUserId(authHeader);
        List<Order> orders = orderService.onlinePayment(userId, addressId, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orders);
    }

    // ─── My Orders ────────────────────────────────────────────────────

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("Authorization") String authHeader) {

        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(orderService.getOrderByUserId(userId));
    }

    // ─── Order By Id (ownership enforced) ────────────────────────────

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int orderId) {

        int userId = extractUserId(authHeader);
        String role = extractRole(authHeader);

        return orderService.getOrderById(orderId)
                .map(order -> {
                    if ("ADMIN".equalsIgnoreCase(role) ||
                            order.getUserId() == userId) {
                        return ResponseEntity.ok(order);
                    }
                    throw new RuntimeException("Access denied: not your order");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Cancel Order ─────────────────────────────────────────────────

    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<Order> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int orderId) {

        int userId = extractUserId(authHeader);
        Order order = orderService.cancelOrder(orderId, userId, authHeader);
        return ResponseEntity.ok(order);
    }

    // ─── Change Status (Admin only) ───────────────────────────────────

    @PutMapping("/status/{orderId}")
    public ResponseEntity<Order> changeStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int orderId,
            @RequestParam String status) {

        requireAdmin(authHeader);
        return ResponseEntity.ok(
                orderService.changeStatus(orderId, status, authHeader));
    }

    // ─── All Orders (Admin only) ──────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestHeader("Authorization") String authHeader) {

        requireAdmin(authHeader);
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // ─── Delete Order (Admin only) ────────────────────────────────────

    @DeleteMapping("/delete/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int orderId) {

        requireAdmin(authHeader);
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
    }

    // ─── Save Address ─────────────────────────────────────────────────

    @PostMapping("/address/save")
    public ResponseEntity<Address> saveAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Address address) {

        int userId = extractUserId(authHeader);
        address.setCustomerId(userId);
        Address saved = orderService.storeAddress(address);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    // ─── Delete Address ─────────────────────────────────────────────────
    
    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<Map<String, String>> deleteAddress(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int addressId) {

        try {

            int userId = extractUserId(authHeader);

            String message =
                    orderService.deleteAddress(addressId, userId);

            return ResponseEntity.ok(Map.of(
                    "message", message
            ));

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    // ─── My Addresses ─────────────────────────────────────────────────

    @GetMapping("/address/my-addresses")
    public ResponseEntity<List<Address>> getMyAddresses(
            @RequestHeader("Authorization") String authHeader) {

        int userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                orderService.getAddressByCustomerId(userId));
    }

    // ─── All Addresses (Admin only) ───────────────────────────────────

    @GetMapping("/address/all")
    public ResponseEntity<List<Address>> getAllAddresses(
            @RequestHeader("Authorization") String authHeader) {

        requireAdmin(authHeader);
        return ResponseEntity.ok(orderService.getAllAddresses());
    }

    // ─── Internal — Orders By User ────────────────────────────────────

    @GetMapping("/my-orders-by-user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(
            @PathVariable int userId) {

        return ResponseEntity.ok(orderService.getOrderByUserId(userId));
    }
}