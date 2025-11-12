package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.state.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repo;

    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }

    public Order createOrder(String customerId, java.util.List<com.example.order.model.OrderItem> items) {
        return createOrder(customerId, items, null);
    }

    public Order createOrder(String customerId, java.util.List<com.example.order.model.OrderItem> items, String idempotencyKey) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        
        // Check for existing order with same idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existing = repo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Order already exists for idempotency key: {}, returning existing order: {}", 
                        idempotencyKey, existing.get().getId());
                return existing.get();
            }
        }
        
        Order o = Order.create(customerId, items, idempotencyKey);
        log.info("Creating order for customer: {} with idempotency key: {}", customerId, idempotencyKey);
        return repo.save(o);
    }

    public Optional<Order> getById(String id) {
        return repo.findById(id);
    }

    public Optional<Order> getByIdempotencyKey(String idempotencyKey) {
        return repo.findByIdempotencyKey(idempotencyKey);
    }

    public List<Order> list(OrderStatus status, int page, int size) {
        return repo.findAll(status, page, size);
    }

    public long count(OrderStatus status) {
        return repo.count(status);
    }

    public Order updateStatus(String id, OrderStatus newStatus, long expectedVersion) {
        Optional<Order> maybe = repo.findById(id);
        if (maybe.isEmpty()) {
            log.warn("Attempted to update status for non-existent order: {}", id);
            throw new IllegalArgumentException("Order not found: " + id);
        }
        Order o = maybe.get();
        
        // Idempotency: If already in the target status, return without error
        if (o.getStatus() == newStatus) {
            log.debug("Order {} already in status {}, returning without update", id, newStatus);
            return o;
        }
        
        if (o.getVersion() != expectedVersion) {
            log.warn("Version mismatch for order {}: expected {}, actual {}", id, expectedVersion, o.getVersion());
            throw new IllegalStateException("Version mismatch: expected " + expectedVersion + ", actual " + o.getVersion());
        }
        if (!OrderStateMachine.canTransition(o.getStatus(), newStatus)) {
            log.warn("Invalid state transition for order {}: {} -> {}", id, o.getStatus(), newStatus);
            throw new IllegalStateException("Invalid state transition from " + o.getStatus() + " to " + newStatus);
        }
        // set item statuses to mirror order status (simplified)
        OrderStatus oldStatus = o.getStatus();
        if (o.getItems() != null) {
            o.getItems().forEach(it -> it.setStatus(newStatus));
        }
        o.setStatus(newStatus);
        log.info("Updated order {} status from {} to {}", id, oldStatus, newStatus);
        return repo.save(o);
    }

    public Order cancelOrder(String id, long expectedVersion) {
        Optional<Order> maybe = repo.findById(id);
        if (maybe.isEmpty()) {
            log.warn("Attempted to cancel non-existent order: {}", id);
            throw new IllegalArgumentException("Order not found: " + id);
        }
        Order o = maybe.get();
        
        // Idempotency: If already cancelled, return without error
        if (o.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order {} already cancelled, returning without update", id);
            return o;
        }
        
        if (o.getVersion() != expectedVersion) {
            log.warn("Version mismatch for order {}: expected {}, actual {}", id, expectedVersion, o.getVersion());
            throw new IllegalStateException("Version mismatch: expected " + expectedVersion + ", actual " + o.getVersion());
        }
        if (!OrderStateMachine.canTransition(o.getStatus(), OrderStatus.CANCELLED)) {
            log.warn("Cannot cancel order {} in status {}", id, o.getStatus());
            throw new IllegalStateException("Cannot cancel order in status " + o.getStatus());
        }
        if (o.getItems() != null) {
            o.getItems().forEach(it -> it.setStatus(OrderStatus.CANCELLED));
        }
        o.setStatus(OrderStatus.CANCELLED);
        log.info("Cancelled order {}", id);
        return repo.save(o);
    }
}
