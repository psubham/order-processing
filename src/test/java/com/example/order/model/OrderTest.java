package com.example.order.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void testCreate_WithItems() {
        String customerId = "customer-123";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 2, new BigDecimal("29.99")),
            new OrderItem("item-2", "Product B", 1, new BigDecimal("15.50"))
        );

        Order order = Order.create(customerId, items);

        assertNotNull(order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(items, order.getItems());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0L, order.getVersion());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertNull(order.getIdempotencyKey());
    }

    @Test
    void testCreate_WithIdempotencyKey() {
        String customerId = "customer-123";
        String idempotencyKey = "key-123";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 1, new BigDecimal("10.00"))
        );

        Order order = Order.create(customerId, items, idempotencyKey);

        assertEquals(idempotencyKey, order.getIdempotencyKey());
    }

    @Test
    void testRecalculateTotal_SingleItem() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 2, new BigDecimal("10.00"))));

        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
    }

    @Test
    void testRecalculateTotal_MultipleItems() {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Product A", 2, new BigDecimal("10.00")),
            new OrderItem("item-2", "Product B", 3, new BigDecimal("5.00"))
        );
        Order order = Order.create("customer-123", items);

        assertEquals(new BigDecimal("35.00"), order.getTotalAmount());
    }

    @Test
    void testRecalculateTotal_EmptyItems() {
        Order order = Order.create("customer-123", Collections.emptyList());
        order.setItems(Collections.emptyList());

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void testRecalculateTotal_NullItems() {
        Order order = new Order();
        order.setItems(null);
        order.recalculateTotal();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void testSetItems_RecalculatesTotal() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"))));

        List<OrderItem> newItems = Arrays.asList(
            new OrderItem("item-2", "Product B", 2, new BigDecimal("15.00"))
        );
        order.setItems(newItems);

        assertEquals(new BigDecimal("30.00"), order.getTotalAmount());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testSetTotalAmount_WithItems_Recalculates() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 2, new BigDecimal("10.00"))));

        // Setting total amount should recalculate if items exist
        order.setTotalAmount(new BigDecimal("999.99"));

        // Should be recalculated to actual total
        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testSetTotalAmount_WithoutItems_AllowsOverride() {
        Order order = new Order();
        order.setItems(null);
        BigDecimal customTotal = new BigDecimal("50.00");

        order.setTotalAmount(customTotal);

        assertEquals(customTotal, order.getTotalAmount());
    }
}

