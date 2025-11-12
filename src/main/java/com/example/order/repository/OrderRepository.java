package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findAll(OrderStatus status, int page, int size);
    List<Order> findByStatus(OrderStatus status, int page, int size);
    long count(OrderStatus status);
    void delete(String id);
}
