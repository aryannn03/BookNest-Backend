package com.cg.order.orderservice.serviceimpl;

import com.cg.order.orderservice.dto.BookResponse;
import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;
import com.cg.order.orderservice.repository.AddressRepository;
import com.cg.order.orderservice.repository.OrderRepository;
import com.cg.order.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    // ─── Send Email Helper ────────────────────────────────────────────────────

    private void sendOrderEmail(int userId,
                                String subject,
                                String body,
                                String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(
                            authServiceUrl + "/auth/profile/" + userId,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );

            Map<String, Object> user = response.getBody();
            if (user == null) return;

            String email = user.get("email").toString();

            String url = UriComponentsBuilder
                    .fromUri(URI.create(notificationServiceUrl +
                            "/notifications/send-email"))
                    .queryParam("toEmail", email)
                    .queryParam("subject", subject)
                    .queryParam("body", body)
                    .toUriString();

            restTemplate.postForObject(url, null, Object.class);

        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }
    }

    // ─── Fetch Book Helper ────────────────────────────────────────────────────

    private BookResponse fetchBook(int bookId) {
        try {
            BookResponse book = restTemplate.getForObject(
                    bookServiceUrl + "/books/" + bookId, BookResponse.class);
            if (book == null) throw new RuntimeException("Book not found");
            return book;
        } catch (RestClientException e) {
            throw new RuntimeException("Book service unavailable");
        }
    }

    // ─── Refund Wallet Helper ─────────────────────────────────────────────────

    private void refundWallet(int userId, double amount, int orderId) {
        try {
            restTemplate.put(
                    walletServiceUrl + "/wallet/refund/" + userId +
                            "?amount=" + amount, null);
        } catch (Exception e) {
            System.out.println("Refund failed for order #" + orderId +
                    ": " + e.getMessage());
        }
    }

    // ─── Build Order Helper ───────────────────────────────────────────────────

    private Order buildOrder(int userId,
                             int bookId,
                             int quantity,
                             String title,
                             double price,
                             double total,
                             String paymentMode,
                             String status,
                             Address address) {
        Order order = new Order();
        order.setUserId(userId);
        order.setBookId(bookId);
        order.setBookTitle(title);
        order.setBookPrice(price);
        order.setQuantity(quantity);
        order.setAmountPaid(total);
        order.setModeOfPayment(paymentMode);
        order.setOrderStatus(status);
        order.setAddress(address);
        return order;
    }

    // ─── Get All Orders ───────────────────────────────────────────────────────

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // ─── Place COD Order ──────────────────────────────────────────────────────

    @Override
    public Order placeOrder(int userId,
                            int bookId,
                            int quantity,
                            int addressId,
                            String authHeader) {

        BookResponse book = fetchBook(bookId);

        if (book.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Order order = buildOrder(userId, bookId, quantity,
                book.getTitle(), book.getPrice(),
                book.getPrice() * quantity,
                "COD", "Placed", address);

        Order saved = orderRepository.save(order);

        try {
            restTemplate.put(
                    bookServiceUrl + "/books/update-stock/" + bookId +
                            "?quantity=" + (book.getStock() - quantity), null);
        } catch (Exception e) {
            System.out.println("Stock update failed: " + e.getMessage());
        }

        try {
            restTemplate.delete(
                    cartServiceUrl + "/cart/user/" + userId + "/clear");
        } catch (Exception e) {
            System.out.println("Cart clear failed: " + e.getMessage());
        }

        sendOrderEmail(
                userId,
                "Order Confirmed - #" + saved.getOrderId(),
                "Hello,\n\n" +
                        "Your order has been placed successfully.\n\n" +
                        "Order ID: #" + saved.getOrderId() + "\n" +
                        "Book: " + book.getTitle() + "\n" +
                        "Quantity: " + quantity + "\n" +
                        "Amount: ₹" + saved.getAmountPaid() + "\n" +
                        "Payment Mode: COD\n" +
                        "Status: Placed\n\n" +
                        "Thank you for shopping with BookNest.",
                authHeader);

        return saved;
    }

    // ─── Wallet Payment ───────────────────────────────────────────────────────

    @Override
    public Order onlinePayment(int userId,
                               int bookId,
                               int quantity,
                               int addressId,
                               String authHeader) {

        BookResponse book = fetchBook(bookId);
        double total = book.getPrice() * quantity;

        if (book.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        // Validate wallet balance BEFORE saving the order
        ResponseEntity<Map<String, Object>> walletResponse =
                restTemplate.exchange(
                        walletServiceUrl + "/wallet/" + userId,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

        Map<String, Object> wallet = walletResponse.getBody();
        if (wallet == null) throw new RuntimeException("Wallet not found");

        double balance = ((Number) wallet.get("currentBalance")).doubleValue();
        if (balance < total) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Order order = buildOrder(userId, bookId, quantity,
                book.getTitle(), book.getPrice(), total,
                "WALLET", "Confirmed", address);

        Order saved = orderRepository.save(order);

        // Deduct wallet — roll back order if payment fails
        try {
            restTemplate.put(
                    walletServiceUrl + "/wallet/pay/" + userId +
                            "?amount=" + total +
                            "&orderId=" + saved.getOrderId(),
                    null);
        } catch (Exception e) {
            orderRepository.deleteById(saved.getOrderId());
            throw new RuntimeException(
                    "Payment failed, order not placed: " + e.getMessage());
        }

        // Post-payment — non-critical, don't fail the order if these fail
        try {
            restTemplate.put(
                    bookServiceUrl + "/books/update-stock/" + bookId +
                            "?quantity=" + (book.getStock() - quantity), null);
        } catch (Exception e) {
            System.out.println("Stock update failed: " + e.getMessage());
        }

        try {
            restTemplate.delete(
                    cartServiceUrl + "/cart/user/" + userId + "/clear");
        } catch (Exception e) {
            System.out.println("Cart clear failed: " + e.getMessage());
        }

        sendOrderEmail(
                userId,
                "Payment Successful - Order #" + saved.getOrderId(),
                "Hello,\n\n" +
                        "Your payment was successful.\n\n" +
                        "Order ID: #" + saved.getOrderId() + "\n" +
                        "Book: " + book.getTitle() + "\n" +
                        "Quantity: " + quantity + "\n" +
                        "Amount Paid: ₹" + saved.getAmountPaid() + "\n" +
                        "Payment Mode: WALLET\n" +
                        "Status: Confirmed\n\n" +
                        "Thank you for shopping with BookNest.",
                authHeader);

        return saved;
    }

    // ─── Change Status ────────────────────────────────────────────────────────

    @Override
    public Order changeStatus(int orderId, String status, String authHeader) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);

        // Refund wallet if admin cancels a WALLET order
        if (status.equalsIgnoreCase("Cancelled") &&
                "WALLET".equalsIgnoreCase(saved.getModeOfPayment())) {
            refundWallet(saved.getUserId(),
                    saved.getAmountPaid(), saved.getOrderId());
        }

        String subject = "";
        String body    = "";

        if (status.equalsIgnoreCase("Dispatched")) {
            subject = "Order Dispatched";
            body = "Hello,\n\n" +
                    "Your order #" + saved.getOrderId() +
                    " has been dispatched.\n" +
                    "Book: " + saved.getBookTitle() + "\n" +
                    "Quantity: " + saved.getQuantity() + "\n\n" +
                    "It will reach you soon.\n\n" +
                    "Thank you for shopping with BookNest.";

        } else if (status.equalsIgnoreCase("Delivered")) {
            subject = "Order Delivered";
            body = "Hello,\n\n" +
                    "Your order #" + saved.getOrderId() +
                    " has been delivered successfully.\n\n" +
                    "We hope you enjoy reading " +
                    saved.getBookTitle() + ".\n\n" +
                    "Thank you for shopping with BookNest.";

        } else if (status.equalsIgnoreCase("Cancelled")) {
            subject = "Order Cancelled";
            body = "Hello,\n\n" +
                    "Your order #" + saved.getOrderId() +
                    " has been cancelled.\n\n" +
                    "If payment was completed, a refund will be processed soon.\n\n" +
                    "BookNest Support Team";
        }

        if (!subject.isEmpty()) {
            sendOrderEmail(saved.getUserId(), subject, body, authHeader);
        }

        return saved;
    }

    // ─── Delete Order ─────────────────────────────────────────────────────────

    @Override
    public void deleteOrder(int orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        orderRepository.deleteById(orderId);
    }

    // ─── Orders By User ───────────────────────────────────────────────────────

    @Override
    public List<Order> getOrderByUserId(int userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Optional<Order> getOrderById(int orderId) {
        return orderRepository.findById(orderId);
    }

    // ─── Address ──────────────────────────────────────────────────────────────

    @Override
    public Address storeAddress(Address address) {
        return addressRepository.save(address);
    }

    @Override
    public List<Address> getAddressByCustomerId(int customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }

    // ─── Cancel Order ─────────────────────────────────────────────────────────

    @Override
    public Order cancelOrder(int orderId, int userId, String authHeader) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getUserId() != userId) {
            throw new RuntimeException("Unauthorized");
        }

        if (order.getOrderStatus().equalsIgnoreCase("Delivered")) {
            throw new RuntimeException(
                    "Cannot cancel an already delivered order");
        }

        order.setOrderStatus("Cancelled");
        Order saved = orderRepository.save(order);

        // Refund wallet if payment was made via wallet
        if ("WALLET".equalsIgnoreCase(saved.getModeOfPayment())) {
            refundWallet(saved.getUserId(),
                    saved.getAmountPaid(), saved.getOrderId());
        }

        sendOrderEmail(
                saved.getUserId(),
                "Order Cancelled",
                "Hello,\n\n" +
                        "Your order #" + saved.getOrderId() +
                        " has been cancelled successfully.\n\n" +
                        "BookNest Team",
                authHeader);

        return saved;
    }
}