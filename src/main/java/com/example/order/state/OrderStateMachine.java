package com.example.order.state;

import com.example.order.model.OrderStatus;

import java.util.*;

public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> allowed = new EnumMap<>(OrderStatus.class);
    static {
        // Forward-only transitions (no reverse allowed)
        allowed.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        allowed.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED));
        allowed.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        allowed.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class)); // Terminal state
        allowed.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)); // Terminal state
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) return false;
        // Allow same status (idempotent - no-op updates)
        if (from == to) return true;
        return allowed.getOrDefault(from, Collections.emptySet()).contains(to);
    }
}
