package com.example.order.dto;

import com.example.order.model.OrderItem;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

public class CreateOrderRequest {
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItem> items;
    // Optional idempotency key - if provided, ensures the same order isn't created twice
    private String idempotencyKey;

    public CreateOrderRequest() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
