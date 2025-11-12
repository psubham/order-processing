package com.example.order.scheduler;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PendingToProcessingScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingToProcessingScheduler.class);
    private final OrderService service;
    private final OrderRepository repo;

    public PendingToProcessingScheduler(OrderService service, OrderRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    // every 5 minutes
    @Scheduled(fixedDelayString = "${order.scheduler.delay:300000}")
    public void run() {
        int page = 0;
        int size = 100;
        List<Order> pageOrders;
        do {
            pageOrders = repo.findByStatus(OrderStatus.PENDING, page, size);
            for (Order o : pageOrders) {
                try {
                    service.updateStatus(o.getId(), OrderStatus.PROCESSING, o.getVersion());
                    log.debug("Successfully moved order {} from PENDING to PROCESSING", o.getId());
                } catch (Exception e) {
                    log.warn("Scheduler: failed to move order {} from PENDING to PROCESSING: {}", 
                            o.getId(), e.getMessage(), e);
                }
            }
            page++;
        } while (!pageOrders.isEmpty());
    }
}
