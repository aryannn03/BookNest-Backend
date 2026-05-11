package com.cg.order.orderservice.repository;

import com.cg.order.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Get all orders by userId
    List<Order> findByUserId(int userId);

    // Get latest order
    Optional<Order> findFirstByOrderByOrderIdDesc();

    // Get orders by status
    List<Order> findByOrderStatus(String orderStatus);

    // Get orders between dates
    List<Order> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);
}