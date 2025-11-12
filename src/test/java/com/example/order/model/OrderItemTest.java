package com.example.order.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void testConstructor_ValidInput() {
        OrderItem item = new OrderItem("item-1", "Product A", 2, new BigDecimal("29.99"));

        assertEquals("item-1", item.getItemId());
        assertEquals("Product A", item.getName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("29.99"), item.getPrice());
        assertEquals(OrderStatus.PENDING, item.getStatus());
    }

    @Test
    void testConstructor_ZeroQuantity_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("item-1", "Product", 0, new BigDecimal("10.00"));
        });
    }

    @Test
    void testConstructor_NegativeQuantity_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("item-1", "Product", -1, new BigDecimal("10.00"));
        });
    }

    @Test
    void testConstructor_NullPrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("item-1", "Product", 1, null);
        });
    }

    @Test
    void testConstructor_NegativePrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("item-1", "Product", 1, new BigDecimal("-10.00"));
        });
    }

    @Test
    void testSetQuantity_Valid() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        item.setQuantity(5);

        assertEquals(5, item.getQuantity());
    }

    @Test
    void testSetQuantity_Zero_ThrowsException() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            item.setQuantity(0);
        });
    }

    @Test
    void testSetQuantity_Negative_ThrowsException() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            item.setQuantity(-1);
        });
    }

    @Test
    void testSetPrice_Valid() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        item.setPrice(new BigDecimal("20.00"));

        assertEquals(new BigDecimal("20.00"), item.getPrice());
    }

    @Test
    void testSetPrice_Zero_Valid() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        item.setPrice(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, item.getPrice());
    }

    @Test
    void testSetPrice_Null_ThrowsException() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            item.setPrice(null);
        });
    }

    @Test
    void testSetPrice_Negative_ThrowsException() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            item.setPrice(new BigDecimal("-10.00"));
        });
    }

    @Test
    void testSetStatus() {
        OrderItem item = new OrderItem("item-1", "Product", 1, new BigDecimal("10.00"));
        item.setStatus(OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, item.getStatus());
    }
}

