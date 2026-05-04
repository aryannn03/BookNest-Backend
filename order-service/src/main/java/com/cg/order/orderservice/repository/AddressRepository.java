package com.cg.order.orderservice.repository;

import com.cg.order.orderservice.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

    // Get all addresses for a customer
    List<Address> findByCustomerId(int customerId);

    // Get addresses by city
    List<Address> findByCity(String city);

    // Delete all addresses for a customer
    void deleteByCustomerId(int customerId);
}