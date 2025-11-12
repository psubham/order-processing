package com.example.order.state;

import com.example.order.model.OrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    @Test
    void testValidTransitions() {
        // PENDING -> PROCESSING
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.PROCESSING));
        
        // PENDING -> CANCELLED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
        
        // PROCESSING -> SHIPPED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED));
        
        // SHIPPED -> DELIVERED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void testInvalidTransitions() {
        // Reverse transitions not allowed
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PROCESSING, OrderStatus.PENDING));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.PROCESSING));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.SHIPPED));
        
        // Skip states not allowed
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.SHIPPED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PROCESSING, OrderStatus.DELIVERED));
        
        // Terminal states cannot transition
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.PROCESSING));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.PENDING));
        
        // CANCELLED can only come from PENDING
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
    }

    @Test
    void testIdempotentTransitions() {
        // Same status should be allowed (idempotent)
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.PENDING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PROCESSING, OrderStatus.PROCESSING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.SHIPPED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.DELIVERED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.CANCELLED));
    }

    @Test
    void testNullHandling() {
        assertFalse(OrderStateMachine.canTransition(null, OrderStatus.PENDING));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PENDING, null));
        assertFalse(OrderStateMachine.canTransition(null, null));
    }
}

