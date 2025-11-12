package com.example.order.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Schema(description = "An item in an order")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    @Schema(description = "Product ID", 
            example = "item-12345",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemId;
    
    @Schema(description = "Product name", 
            example = "Wireless Mouse",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Quantity", 
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;
    
    @Schema(description = "Unit price", 
            example = "29.99",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;
    
    @Schema(description = "Item status", 
            example = "PENDING",
            accessMode = Schema.AccessMode.READ_ONLY)
    private OrderStatus status;

    public OrderItem(String itemId, String name, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.status = OrderStatus.PENDING;
    }

    // Custom setters with validation
    public void setQuantity(int quantity) { 
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity; 
    }
    
    public void setPrice(BigDecimal price) { 
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        this.price = price; 
    }
}
