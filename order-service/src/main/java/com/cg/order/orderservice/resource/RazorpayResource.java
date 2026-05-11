package com.cg.order.orderservice.resource;

import com.cg.order.orderservice.client.BookClient;
import com.cg.order.orderservice.client.CartClient;
import com.cg.order.orderservice.dto.BookResponse;
import com.cg.order.orderservice.dto.RazorpayOrderRequest;
import com.cg.order.orderservice.dto.RazorpayOrderResponse;
import com.cg.order.orderservice.dto.RazorpayVerifyRequest;
import com.cg.order.orderservice.entity.Order;
import com.cg.order.orderservice.security.JwtUtil;
import com.cg.order.orderservice.service.OrderService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders/razorpay")
public class RazorpayResource {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookClient bookClient;

    @Autowired
    private CartClient cartClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ─── Helper: extract userId from token ───────────────────────────────────

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

    // ─── Helper: fetch all cart items for a user ─────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchCartItems(int userId) {
        try {
            Map<String, Object> cart = cartClient.getCartByUserId(userId);
            if (cart == null) return List.of();
            return (List<Map<String, Object>>) cart.get("items");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cart service unavailable: " + e.getMessage());
        }
    }

    // ─── Step 1: Create Razorpay Order ───────────────────────────────────────

    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RazorpayOrderRequest request) {

        int userId = extractUserId(authHeader);

        List<Map<String, Object>> cartItems = fetchCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cart is empty"));
        }

        double totalAmount = 0;
        for (Map<String, Object> item : cartItems) {
            int bookId   = ((Number) item.get("bookId")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();

            BookResponse book;
            try {
                book = bookClient.getBookById(bookId);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Book service unavailable"));
            }

            if (book == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Book not found: " + bookId));
            }
            if (book.getStock() < quantity) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Insufficient stock for: " + book.getTitle()));
            }

            totalAmount += book.getPrice() * quantity;
        }

        long amountInPaise = (long) (totalAmount * 100);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booknest_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder =
                    razorpayClient.orders.create(orderRequest);

            RazorpayOrderResponse response = new RazorpayOrderResponse(
                    razorpayOrder.get("id"),
                    totalAmount,
                    "INR",
                    razorpayKeyId
            );

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                            "Razorpay order creation failed: " + e.getMessage()));
        }
    }

    // ─── Step 2: Verify Signature & Place Orders ──────────────────────────────

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndPlaceOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RazorpayVerifyRequest request) {

        int userId = extractUserId(authHeader);

        try {
            String payload = request.getRazorpayOrderId()
                    + "|" + request.getRazorpayPaymentId();

            String generatedSignature = Utils.getHash(payload, razorpayKeySecret);

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Payment verification failed: invalid signature"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                            "Signature verification error: " + e.getMessage()));
        }

        List<Order> savedOrders = orderService.placeRazorpayOrder(
                userId,
                request.getAddressId(),
                request.getRazorpayPaymentId(),
                authHeader
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrders);
    }
}