package com.example.order.dto;

import com.example.order.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a new order")
public class CreateOrderRequest {
    @NotBlank(message = "Customer ID is required")
    @Schema(description = "Customer ID", 
            example = "C14325",
            required = true)
    private String customerId;
    
    @NotEmpty(message = "Order must have at least one item")
    @Schema(description = "List of items to order (at least one required)", 
            required = true)
    private List<OrderItem> items;

    public CreateOrderRequest() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
