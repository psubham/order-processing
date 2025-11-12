package com.example.order.state;

import com.example.order.model.OrderStatus;

import java.util.*;

public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> allowed = new EnumMap<>(OrderStatus.class);
    static {
        allowed.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        allowed.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED));
        allowed.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        allowed.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        allowed.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) return false;
        return allowed.getOrDefault(from, Collections.emptySet()).contains(to);
    }
}
