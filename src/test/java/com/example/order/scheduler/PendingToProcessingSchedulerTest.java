package com.example.order.scheduler;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingToProcessingSchedulerTest {

    @Mock
    private OrderService service;

    @Mock
    private OrderRepository repository;

    private PendingToProcessingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PendingToProcessingScheduler(service, repository);
    }

    @Test
    void testRun_ProcessesPendingOrders() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));

        List<Order> firstPage = Arrays.asList(order1, order2);
        List<Order> secondPage = Collections.emptyList();

        when(repository.findByStatus(OrderStatus.PENDING, 0, 100)).thenReturn(firstPage);
        when(repository.findByStatus(OrderStatus.PENDING, 1, 100)).thenReturn(secondPage);

        scheduler.run();

        verify(service, times(1)).updateStatus(order1.getId(), OrderStatus.PROCESSING, order1.getVersion());
        verify(service, times(1)).updateStatus(order2.getId(), OrderStatus.PROCESSING, order2.getVersion());
    }

    @Test
    void testRun_HandlesEmptyPendingOrders() {
        when(repository.findByStatus(OrderStatus.PENDING, 0, 100)).thenReturn(Collections.emptyList());

        scheduler.run();

        verify(service, never()).updateStatus(anyString(), any(), anyLong());
    }

    @Test
    void testRun_HandlesPagination() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));

        List<Order> firstPage = Arrays.asList(order1);
        List<Order> secondPage = Arrays.asList(order2);
        List<Order> thirdPage = Collections.emptyList();

        when(repository.findByStatus(OrderStatus.PENDING, 0, 100)).thenReturn(firstPage);
        when(repository.findByStatus(OrderStatus.PENDING, 1, 100)).thenReturn(secondPage);
        when(repository.findByStatus(OrderStatus.PENDING, 2, 100)).thenReturn(thirdPage);

        scheduler.run();

        verify(service, times(1)).updateStatus(order1.getId(), OrderStatus.PROCESSING, order1.getVersion());
        verify(service, times(1)).updateStatus(order2.getId(), OrderStatus.PROCESSING, order2.getVersion());
    }

    @Test
    void testRun_HandlesServiceException() {
        Order order = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));

        when(repository.findByStatus(OrderStatus.PENDING, 0, 100)).thenReturn(Arrays.asList(order));
        when(repository.findByStatus(OrderStatus.PENDING, 1, 100)).thenReturn(Collections.emptyList());
        doThrow(new IllegalStateException("Version mismatch"))
            .when(service).updateStatus(order.getId(), OrderStatus.PROCESSING, order.getVersion());

        // Should not throw exception, just log warning
        scheduler.run();

        verify(service, times(1)).updateStatus(order.getId(), OrderStatus.PROCESSING, order.getVersion());
    }
}

