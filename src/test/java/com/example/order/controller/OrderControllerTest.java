package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.PageResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService service;

    @InjectMocks
    private OrderController controller;

    @BeforeEach
    void setUp() {
        // Controller is already injected via @InjectMocks
    }

    @Test
    void testCreate_NewOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("customer-123");
        request.setItems(Arrays.asList(
            new OrderItem("item-1", "Product A", 2, new BigDecimal("29.99"))
        ));

        Order order = Order.create("customer-123", request.getItems());
        when(service.createOrder(eq("customer-123"), anyList(), isNull())).thenReturn(order);

        ResponseEntity<Order> response = controller.create(null, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testCreate_WithIdempotencyKey_Existing() {
        String idempotencyKey = "key-123";
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("customer-123");
        request.setItems(Arrays.asList(
            new OrderItem("item-1", "Product A", 1, new BigDecimal("10.00"))
        ));

        Order existingOrder = Order.create("customer-123", request.getItems(), idempotencyKey);
        when(service.getByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));
        when(service.createOrder(eq("customer-123"), anyList(), eq(idempotencyKey))).thenReturn(existingOrder);

        ResponseEntity<Order> response = controller.create(idempotencyKey, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(existingOrder, response.getBody());
    }

    @Test
    void testGet_Found() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        when(service.getById(orderId)).thenReturn(Optional.of(order));

        ResponseEntity<Order> response = controller.get(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(order, response.getBody());
    }

    @Test
    void testGet_NotFound() {
        String orderId = "non-existent";
        when(service.getById(orderId)).thenReturn(Optional.empty());

        ResponseEntity<Order> response = controller.get(orderId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGet_InvalidId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.get(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            controller.get("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            controller.get("a".repeat(101)); // Too long
        });
    }

    @Test
    void testList_WithStatus() {
        OrderStatus status = OrderStatus.PENDING;
        int page = 0;
        int size = 20;
        List<Order> orders = Arrays.asList(
            Order.create("customer-1", Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)))
        );

        when(service.list(status, page, size)).thenReturn(orders);
        when(service.count(status)).thenReturn(1L);

        PageResponse<Order> response = controller.list(status, page, size);

        assertEquals(orders, response.getContent());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertEquals(1L, response.getTotalElements());
    }

    @Test
    void testList_WithoutStatus() {
        int page = 0;
        int size = 20;
        List<Order> orders = Collections.emptyList();

        when(service.list(null, page, size)).thenReturn(orders);
        when(service.count(null)).thenReturn(0L);

        PageResponse<Order> response = controller.list(null, page, size);

        assertEquals(orders, response.getContent());
        assertEquals(0L, response.getTotalElements());
    }

    @Test
    void testList_InvalidPage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.list(null, -1, 20);
        });
    }

    @Test
    void testList_InvalidSize_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.list(null, 0, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            controller.list(null, 0, 1001); // Exceeds MAX_PAGE_SIZE
        });
    }

    @Test
    void testUpdateStatus_Success() {
        String orderId = "order-123";
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(OrderStatus.PROCESSING);
        request.setVersion(1L);

        Order updatedOrder = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        updatedOrder.setStatus(OrderStatus.PROCESSING);
        updatedOrder.setVersion(2L);

        when(service.updateStatus(orderId, OrderStatus.PROCESSING, 1L)).thenReturn(updatedOrder);

        ResponseEntity<Order> response = controller.updateStatus(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedOrder, response.getBody());
    }

    @Test
    void testCancel_Success() {
        String orderId = "order-123";
        long version = 1L;

        Order cancelledOrder = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        when(service.cancelOrder(orderId, version)).thenReturn(cancelledOrder);

        ResponseEntity<Order> response = controller.cancel(orderId, version);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cancelledOrder, response.getBody());
    }
}

