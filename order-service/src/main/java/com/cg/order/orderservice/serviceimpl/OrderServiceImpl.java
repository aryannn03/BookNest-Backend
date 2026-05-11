package com.cg.order.orderservice.serviceimpl;

import com.cg.order.orderservice.client.AuthClient;
import com.cg.order.orderservice.client.BookClient;
import com.cg.order.orderservice.client.CartClient;
import com.cg.order.orderservice.client.NotificationClient;
import com.cg.order.orderservice.client.WalletClient;
import com.cg.order.orderservice.config.RabbitMQConfig;
import com.cg.order.orderservice.dto.BookResponse;
import com.cg.order.orderservice.dto.OrderEvent;
import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;
import com.cg.order.orderservice.repository.AddressRepository;
import com.cg.order.orderservice.repository.OrderRepository;
import com.cg.order.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BookClient bookClient;

    @Autowired
    private CartClient cartClient;

    @Autowired
    private WalletClient walletClient;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private NotificationClient notificationClient;

    // ─── Send In-App Notification Helper ─────────────────────────────────────

    private void sendNotification(int userId, String type, String message) {
        try {
            String encodedMessage = java.net.URLEncoder.encode(
                    message, java.nio.charset.StandardCharsets.UTF_8);
            notificationClient.sendNotification(userId, type, encodedMessage);
        } catch (Exception e) {
            log.warn("In-app notification failed: {}", e.getMessage());
        }
    }

    // ─── Get User Email Helper ────────────────────────────────────────────────

    private String getUserEmail(int userId, String authHeader) {
        try {
            Map<String, Object> user = authClient.getUserProfile(
                    authHeader, userId);
            return user != null ? user.get("email").toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Publish Order Event Helper ───────────────────────────────────────────

    private void publishOrderEvent(Order order, String eventType,
                                   String userEmail) {
        try {
            OrderEvent event = new OrderEvent(
                order.getOrderId(),
                order.getUserId(),
                userEmail,
                order.getBookTitle(),
                order.getQuantity(),
                order.getAmountPaid(),
                order.getModeOfPayment(),
                order.getOrderStatus(),
                eventType
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                event
            );
        } catch (Exception e) {
            log.warn("RabbitMQ publish failed: {}", e.getMessage());
        }
    }

    // ─── Fetch Book Helper ────────────────────────────────────────────────────

    private BookResponse fetchBook(int bookId) {
        try {
            BookResponse book = bookClient.getBookById(bookId);
            if (book == null) throw new RuntimeException("Book not found");
            return book;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Book service unavailable");
        }
    }

    // ─── Fetch Cart Items Helper ──────────────────────────────────────────────

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

    // ─── Refund Wallet Helper ─────────────────────────────────────────────────

    private void refundWallet(int userId, double amount, int orderId) {
        try {
            walletClient.refundMoney(userId, amount);
        } catch (Exception e) {
            log.warn("Refund failed for order #{}: {}", orderId, e.getMessage());
        }
    }

    // ─── Build Order Helper ───────────────────────────────────────────────────

    private Order buildOrder(int userId, int bookId, int quantity,
                             String title, double price, double total,
                             String paymentMode, String status,
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
    public List<Order> placeOrder(int userId, int addressId,
                                  String authHeader) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        List<Map<String, Object>> cartItems = fetchCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        List<Order> savedOrders = new ArrayList<>();

        for (Map<String, Object> item : cartItems) {
            int bookId   = ((Number) item.get("bookId")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            BookResponse book = fetchBook(bookId);

            if (book.getStock() < quantity) {
                throw new RuntimeException(
                        "Insufficient stock for: " + book.getTitle());
            }

            Order order = buildOrder(userId, bookId, quantity,
                    book.getTitle(), book.getPrice(),
                    book.getPrice() * quantity,
                    "COD", "Placed", address);

            Order saved = orderRepository.save(order);
            savedOrders.add(saved);

            try {
                bookClient.updateStock(bookId,
                        book.getStock() - quantity);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Stock update failed: " + e.getMessage());
            }
        }

        try {
            cartClient.clearCart(userId);
        } catch (Exception e) {
            throw new RuntimeException("Cart clear failed: " + e.getMessage());
        }

        sendNotification(userId, "ORDER_PLACED",
                savedOrders.size() + " order(s) placed successfully via COD!");

        String email = getUserEmail(userId, authHeader);
        for (Order saved : savedOrders) {
            publishOrderEvent(saved, "ORDER_PLACED", email);
        }

        return savedOrders;
    }

    // ─── Wallet Payment ───────────────────────────────────────────────────────

    @Override
    public List<Order> onlinePayment(int userId, int addressId,
                                     String authHeader) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        List<Map<String, Object>> cartItems = fetchCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double grandTotal = 0;
        List<BookResponse> books = new ArrayList<>();

        for (Map<String, Object> item : cartItems) {
            int bookId   = ((Number) item.get("bookId")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            BookResponse book = fetchBook(bookId);

            if (book.getStock() < quantity) {
                throw new RuntimeException(
                        "Insufficient stock for: " + book.getTitle());
            }

            grandTotal += book.getPrice() * quantity;
            books.add(book);
        }

        // Check wallet balance
        Map<String, Object> wallet = walletClient.getWallet(userId);
        if (wallet == null) throw new RuntimeException("Wallet not found");

        double balance = ((Number) wallet.get("currentBalance")).doubleValue();
        if (balance < grandTotal) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        // Save orders and update stock
        List<Order> savedOrders = new ArrayList<>();

        for (int i = 0; i < cartItems.size(); i++) {
            Map<String, Object> item = cartItems.get(i);
            int bookId   = ((Number) item.get("bookId")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            BookResponse book = books.get(i);

            Order order = buildOrder(userId, bookId, quantity,
                    book.getTitle(), book.getPrice(),
                    book.getPrice() * quantity,
                    "WALLET", "Placed", address);

            Order saved = orderRepository.save(order);
            savedOrders.add(saved);

            try {
                bookClient.updateStock(bookId,
                        book.getStock() - quantity);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Stock update failed: " + e.getMessage());
            }
        }

        // Deduct wallet with real orderId
        int firstOrderId = savedOrders.get(0).getOrderId();
        try {
            walletClient.payMoney(userId, grandTotal, firstOrderId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Wallet payment failed: " + e.getMessage());
        }

        try {
            cartClient.clearCart(userId);
        } catch (Exception e) {
            throw new RuntimeException("Cart clear failed: " + e.getMessage());
        }

        sendNotification(userId, "ORDER_PLACED",
                savedOrders.size() + " order(s) placed via Wallet!");
        sendNotification(userId, "PAYMENT_SUCCESS",
                "Payment of ₹" + String.format("%.2f", grandTotal)
                + " via Wallet was successful!");

        String email = getUserEmail(userId, authHeader);
        for (Order saved : savedOrders) {
            publishOrderEvent(saved, "ORDER_PLACED", email);
        }

        return savedOrders;
    }

    // ─── Razorpay Order ───────────────────────────────────────────────────────

    @Override
    public List<Order> placeRazorpayOrder(int userId, int addressId,
                                           String razorpayPaymentId,
                                           String authHeader) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        List<Map<String, Object>> cartItems = fetchCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        List<Order> savedOrders = new ArrayList<>();

        for (Map<String, Object> item : cartItems) {
            int bookId   = ((Number) item.get("bookId")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            BookResponse book = fetchBook(bookId);

            if (book.getStock() < quantity) {
                throw new RuntimeException(
                        "Insufficient stock for: " + book.getTitle());
            }

            Order order = buildOrder(userId, bookId, quantity,
                    book.getTitle(), book.getPrice(),
                    book.getPrice() * quantity,
                    "RAZORPAY", "Placed", address);

            order.setRazorpayPaymentId(razorpayPaymentId);

            Order saved = orderRepository.save(order);
            savedOrders.add(saved);

            try {
                bookClient.updateStock(bookId,
                        book.getStock() - quantity);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Stock update failed: " + e.getMessage());
            }
        }

        try {
            cartClient.clearCart(userId);
        } catch (Exception e) {
            throw new RuntimeException("Cart clear failed: " + e.getMessage());
        }

        sendNotification(userId, "ORDER_PLACED",
                savedOrders.size() + " order(s) placed via Razorpay!");
        sendNotification(userId, "PAYMENT_SUCCESS",
                "Razorpay payment " + razorpayPaymentId + " confirmed!");

        String email = getUserEmail(userId, authHeader);
        for (Order saved : savedOrders) {
            publishOrderEvent(saved, "ORDER_PLACED", email);
        }

        return savedOrders;
    }

    // ─── Change Status ────────────────────────────────────────────────────────

    @Override
    public Order changeStatus(int orderId, String status,
                              String authHeader) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);

        if (status.equalsIgnoreCase("Cancelled") &&
                "WALLET".equalsIgnoreCase(saved.getModeOfPayment())) {
            refundWallet(saved.getUserId(),
                    saved.getAmountPaid(), saved.getOrderId());
        }

        String notifType = "";
        String notifMessage = "";

        if (status.equalsIgnoreCase("Dispatched")) {
            notifType    = "ORDER_DISPATCHED";
            notifMessage = "Your order #" + saved.getOrderId()
                    + " for \"" + saved.getBookTitle()
                    + "\" has been dispatched!";
        } else if (status.equalsIgnoreCase("Delivered")) {
            notifType    = "ORDER_DELIVERED";
            notifMessage = "Your order #" + saved.getOrderId()
                    + " for \"" + saved.getBookTitle()
                    + "\" has been delivered!";
        } else if (status.equalsIgnoreCase("Cancelled")) {
            notifType    = "ORDER_CANCELLED";
            notifMessage = "Your order #" + saved.getOrderId()
                    + " for \"" + saved.getBookTitle()
                    + "\" has been cancelled.";
        }

        if (!notifType.isEmpty()) {
            sendNotification(saved.getUserId(), notifType, notifMessage);
        }

        String email = getUserEmail(saved.getUserId(), authHeader);
        publishOrderEvent(saved, "STATUS_CHANGED", email);

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

    @Override
    public String deleteAddress(int addressId, int userId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new RuntimeException("Address not found"));

        if (address.getCustomerId() != userId) {
            throw new RuntimeException(
                    "Access denied: not your address");
        }

        try {
            addressRepository.delete(address);
            return "Address deleted successfully";

        } catch (DataIntegrityViolationException e) {

            throw new RuntimeException(
                    "Cannot delete address used in previous orders");
        }
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

        if ("WALLET".equalsIgnoreCase(saved.getModeOfPayment())) {
            refundWallet(saved.getUserId(),
                    saved.getAmountPaid(), saved.getOrderId());
        }

        sendNotification(saved.getUserId(), "ORDER_CANCELLED",
                "Your order #" + saved.getOrderId()
                + " for \"" + saved.getBookTitle()
                + "\" has been cancelled.");

        String email = getUserEmail(saved.getUserId(), authHeader);
        publishOrderEvent(saved, "ORDER_CANCELLED", email);

        return saved;
    }
}