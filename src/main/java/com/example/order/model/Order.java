package com.example.order.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Schema(description = "An order")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Schema(description = "Order ID", 
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String id;
    
    @Schema(description = "Customer ID", 
            example = "customer-123",
            required = true)
    private String customerId;
    
    @Schema(description = "Current order status", 
            example = "PENDING",
            allowableValues = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"},
            required = true)
    private OrderStatus status;
    
    @Schema(description = "Order items", 
            required = true)
    private List<OrderItem> items;
    
    @Schema(description = "Total amount (calculated)", 
            example = "75.48",
            accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal totalAmount;
    
    @Schema(description = "Version number (needed for updates)", 
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY,
            minimum = "0")
    private long version;
    
    @Schema(description = "Creation timestamp", 
            example = "2024-01-15T10:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
    
    @Schema(description = "Last update timestamp", 
            example = "2024-01-15T10:35:00Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
    
    @Schema(description = "Idempotency key (if used)", 
            example = "550e8400-e29b-41d4-a716-446655440000",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String idempotencyKey;

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

    // Custom setter with business logic
    public void setItems(List<OrderItem> items) { 
        this.items = items; 
        recalculateTotal(); 
    }
    
    // Custom setter for JSON deserialization compatibility
    @Deprecated
    public void setTotalAmount(BigDecimal totalAmount) { 
        // Recalculate to ensure consistency, but allow override for deserialization
        if (items != null && !items.isEmpty()) {
            recalculateTotal();
        } else {
            this.totalAmount = totalAmount;
        }
    }
}
