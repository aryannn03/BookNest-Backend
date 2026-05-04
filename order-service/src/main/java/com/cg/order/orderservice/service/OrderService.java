package com.cg.order.orderservice.service;

import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    List<Order> getAllOrders();

    // COD
    Order placeOrder(int userId, int bookId,
                     int quantity, int addressId,
                     String authHeader);

    // Wallet
    Order onlinePayment(int userId, int bookId,
                        int quantity, int addressId,
                        String authHeader);

    // authHeader needed to fetch user email for notification
    Order changeStatus(int orderId, String status, String authHeader);

    void deleteOrder(int orderId);

    List<Order> getOrderByUserId(int userId);

    Optional<Order> getOrderById(int orderId);

    Address storeAddress(Address address);

    List<Address> getAddressByCustomerId(int customerId);

    List<Address> getAllAddresses();

    Order cancelOrder(int orderId, int userId, String authHeader);
}