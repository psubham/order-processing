package com.example.order.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public class Order {
    private String id;
    private String customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
    private String idempotencyKey;

    public Order() {}

    public static Order create(String customerId, List<OrderItem> items) {
        return create(customerId, items, null);
    }

    public static Order create(String customerId, List<OrderItem> items, String idempotencyKey) {
        Order o = new Order();
        o.id = UUID.randomUUID().toString();
        o.customerId = customerId;
        o.items = items;
        o.status = OrderStatus.PENDING;
        o.version = 0L;
        o.createdAt = Instant.now();
        o.updatedAt = Instant.now();
        o.idempotencyKey = idempotencyKey;
        o.recalculateTotal();
        return o;
    }

    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItem it : items) {
                sum = sum.add(it.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }
        this.totalAmount = sum;
    }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; recalculateTotal(); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    // Note: setTotalAmount is deprecated - total is calculated from items
    // This setter is kept for JSON deserialization compatibility
    @Deprecated
    public void setTotalAmount(BigDecimal totalAmount) { 
        // Recalculate to ensure consistency, but allow override for deserialization
        if (items != null && !items.isEmpty()) {
            recalculateTotal();
        } else {
            this.totalAmount = totalAmount;
        }
    }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
