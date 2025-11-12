package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.PageResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management API - Create, retrieve, update, and cancel orders")
public class OrderController {
    private static final int MAX_PAGE_SIZE = 1000;
    
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create a new order",
            description = "Create a new order. Starts with PENDING status. Use Idempotency-Key header to prevent duplicates on retry."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "200", description = "Order already exists (idempotency key match)",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<Order> create(
            @Parameter(
                    name = "Idempotency-Key",
                    description = "Optional header to prevent duplicate orders on retry",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    schema = @Schema(type = "string", format = "uuid"))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "Order details")
            @Validated @RequestBody CreateOrderRequest req) {
        // Check if order already exists with this idempotency key
        boolean orderExists = idempotencyKey != null && 
                              service.getByIdempotencyKey(idempotencyKey).isPresent();
        
        Order o = service.createOrder(req.getCustomerId(), req.getItems(), idempotencyKey);
        // Return 200 if order already existed (idempotency), 201 if newly created
        return ResponseEntity.status(orderExists ? 200 : 201).body(o);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Get order details by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Order> get(
            @Parameter(
                    name = "id",
                    description = "Order ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000",
                    schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id) {
        // Basic UUID format validation
        if (id == null || id.isBlank() || id.length() > 100) {
            throw new IllegalArgumentException("Invalid order ID format");
        }
        return service.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "List orders with pagination",
            description = "Get paginated list of orders. Optionally filter by status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @GetMapping
    public PageResponse<Order> list(
            @Parameter(
                    name = "status",
                    description = "Filter by status (optional)",
                    example = "PENDING",
                    schema = @Schema(allowableValues = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"}))
            @RequestParam(required = false) OrderStatus status,
            @Parameter(
                    name = "page",
                    description = "Page number (starts at 0)",
                    example = "0",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(
                    name = "size",
                    description = "Items per page (max 1000)",
                    example = "20",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "1000", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<Order> content = service.list(status, page, size);
        long total = service.count(status);
        return new PageResponse<>(content, page, size, total);
    }

    @Operation(
            summary = "Update order status",
            description = "Change order status. Requires current version number."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or order not found"),
            @ApiResponse(responseCode = "409", description = "Version mismatch or invalid state transition")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @Parameter(
                    name = "id",
                    description = "Order ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000",
                    schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id,
            @Parameter(description = "Status and version")
            @Validated @RequestBody UpdateStatusRequest req) {
        Order updated = service.updateStatus(id, req.getStatus(), req.getVersion());
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancel an order. Only PENDING orders can be cancelled. Requires version number."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or order not found"),
            @ApiResponse(responseCode = "409", description = "Version mismatch or order cannot be cancelled in current status")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Order> cancel(
            @Parameter(
                    name = "id",
                    description = "Order ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000",
                    schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id,
            @Parameter(
                    name = "version",
                    description = "Order version (get from order details)",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", minimum = "0"))
            @RequestParam long version) {
        Order cancelled = service.cancelOrder(id, version);
        return ResponseEntity.ok(cancelled);
    }
}
