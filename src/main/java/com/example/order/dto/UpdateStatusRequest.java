package com.example.order.dto;

import com.example.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Schema(description = "Request to update order status")
@Data
@NoArgsConstructor
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    @Schema(description = "New status for the order", 
            example = "PROCESSING",
            required = true,
            allowableValues = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"})
    private OrderStatus status;
    
    @Min(value = 0, message = "Version must be non-negative")
    @Schema(description = "Current order version (get from order details)",
            example = "1",
            required = true,
            minimum = "0")
    private long version;
}
