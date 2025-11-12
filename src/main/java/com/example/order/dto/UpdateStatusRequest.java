package com.example.order.dto;

import com.example.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;
    @Min(value = 0, message = "Version must be non-negative")
    private long version;

    public UpdateStatusRequest() {}

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
