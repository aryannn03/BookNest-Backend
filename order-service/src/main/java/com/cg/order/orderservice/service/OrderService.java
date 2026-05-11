package com.cg.order.orderservice.service;

import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    // ─── Orders ───────────────────────────────────────────────────────────────

    List<Order> placeOrder(int userId, int addressId, String authHeader);

    List<Order> onlinePayment(int userId, int addressId, String authHeader);

    List<Order> placeRazorpayOrder(int userId, int addressId,
                                    String razorpayPaymentId, String authHeader);

    Order cancelOrder(int orderId, int userId, String authHeader);

    Order changeStatus(int orderId, String status, String authHeader);

    void deleteOrder(int orderId);

    List<Order> getAllOrders();

    List<Order> getOrderByUserId(int userId);

    Optional<Order> getOrderById(int orderId);

    // ─── Address ──────────────────────────────────────────────────────────────

    Address storeAddress(Address address);

    List<Address> getAddressByCustomerId(int customerId);

    List<Address> getAllAddresses();
    
    String deleteAddress(int addressId, int userId);
}