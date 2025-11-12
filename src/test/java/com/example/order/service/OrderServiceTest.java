package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(repository);
    }

    @Test
    void testCreateOrder_Success() {
        String customerId = "customer-123";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 2, new BigDecimal("29.99"))
        );

        Order savedOrder = Order.create(customerId, items);
        when(repository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = service.createOrder(customerId, items);

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(repository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_WithIdempotencyKey_NewOrder() {
        String customerId = "customer-123";
        String idempotencyKey = "key-123";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 1, new BigDecimal("10.00"))
        );

        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        Order savedOrder = Order.create(customerId, items, idempotencyKey);
        when(repository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = service.createOrder(customerId, items, idempotencyKey);

        assertNotNull(result);
        verify(repository, times(1)).findByIdempotencyKey(idempotencyKey);
        verify(repository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_WithIdempotencyKey_ExistingOrder() {
        String customerId = "customer-123";
        String idempotencyKey = "key-123";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 1, new BigDecimal("10.00"))
        );

        Order existingOrder = Order.create(customerId, items, idempotencyKey);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));

        Order result = service.createOrder(customerId, items, idempotencyKey);

        assertEquals(existingOrder, result);
        verify(repository, times(1)).findByIdempotencyKey(idempotencyKey);
        verify(repository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_EmptyItems_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createOrder("customer-123", Collections.emptyList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            service.createOrder("customer-123", null);
        });
    }

    @Test
    void testGetById() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        Optional<Order> result = service.getById(orderId);

        assertTrue(result.isPresent());
        assertEquals(order, result.get());
    }

    @Test
    void testGetById_NotFound() {
        String orderId = "non-existent";
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        Optional<Order> result = service.getById(orderId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateStatus_Success() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);
        order.setStatus(OrderStatus.PENDING);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.updateStatus(orderId, OrderStatus.PROCESSING, 1L);

        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        verify(repository, times(1)).save(any(Order.class));
    }

    @Test
    void testUpdateStatus_Idempotent() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);
        order.setStatus(OrderStatus.PROCESSING);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = service.updateStatus(orderId, OrderStatus.PROCESSING, 1L);

        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        verify(repository, never()).save(any(Order.class));
    }

    @Test
    void testUpdateStatus_VersionMismatch_ThrowsException() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(2L);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> {
            service.updateStatus(orderId, OrderStatus.PROCESSING, 1L);
        });
    }

    @Test
    void testUpdateStatus_InvalidTransition_ThrowsException() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);
        order.setStatus(OrderStatus.PROCESSING);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> {
            service.updateStatus(orderId, OrderStatus.PENDING, 1L);
        });
    }

    @Test
    void testUpdateStatus_OrderNotFound_ThrowsException() {
        String orderId = "non-existent";
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateStatus(orderId, OrderStatus.PROCESSING, 1L);
        });
    }

    @Test
    void testCancelOrder_Success() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);
        order.setStatus(OrderStatus.PENDING);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.cancelOrder(orderId, 1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(repository, times(1)).save(any(Order.class));
    }

    @Test
    void testCancelOrder_AlreadyCancelled_Idempotent() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setStatus(OrderStatus.CANCELLED);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = service.cancelOrder(orderId, 1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(repository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrder_InvalidStatus_ThrowsException() {
        String orderId = "order-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);
        order.setStatus(OrderStatus.PROCESSING);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> {
            service.cancelOrder(orderId, 1L);
        });
    }

    @Test
    void testList() {
        OrderStatus status = OrderStatus.PENDING;
        int page = 0;
        int size = 20;
        List<Order> orders = Arrays.asList(
            Order.create("customer-1", Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)))
        );

        when(repository.findAll(status, page, size)).thenReturn(orders);

        List<Order> result = service.list(status, page, size);

        assertEquals(orders, result);
    }

    @Test
    void testCount() {
        OrderStatus status = OrderStatus.PENDING;
        when(repository.count(status)).thenReturn(5L);

        long result = service.count(status);

        assertEquals(5L, result);
    }
}

