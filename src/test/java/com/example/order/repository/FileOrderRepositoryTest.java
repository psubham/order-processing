package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileOrderRepositoryTest {

    @TempDir
    Path tempDir;

    private FileOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FileOrderRepository(tempDir);
    }

    @Test
    void testSave_NewOrder() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));

        Order saved = repository.save(order);

        assertNotNull(saved);
        assertEquals(order.getId(), saved.getId());
        assertEquals(order.getCustomerId(), saved.getCustomerId());
    }

    @Test
    void testSave_UpdateExistingOrder() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order.setVersion(1L);

        Order saved1 = repository.save(order);
        order.setStatus(OrderStatus.PROCESSING);
        order.setVersion(2L);
        Order saved2 = repository.save(order);

        assertEquals(saved1.getId(), saved2.getId());
        assertEquals(OrderStatus.PROCESSING, saved2.getStatus());
        assertEquals(3L, saved2.getVersion());
    }

    @Test
    void testFindById_Existing() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));

        repository.save(order);
        Optional<Order> found = repository.findById(order.getId());

        assertTrue(found.isPresent());
        assertEquals(order.getId(), found.get().getId());
        assertEquals(order.getCustomerId(), found.get().getCustomerId());
    }

    @Test
    void testFindById_NonExistent() {
        Optional<Order> found = repository.findById("non-existent-id");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindByIdempotencyKey_Existing() {
        String idempotencyKey = "key-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)), 
            idempotencyKey);

        repository.save(order);
        Optional<Order> found = repository.findByIdempotencyKey(idempotencyKey);

        assertTrue(found.isPresent());
        assertEquals(idempotencyKey, found.get().getIdempotencyKey());
    }

    @Test
    void testFindByIdempotencyKey_NonExistent() {
        Optional<Order> found = repository.findByIdempotencyKey("non-existent-key");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll_WithStatus() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order1.setStatus(OrderStatus.PENDING);
        
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));
        order2.setStatus(OrderStatus.PROCESSING);

        repository.save(order1);
        repository.save(order2);

        List<Order> pendingOrders = repository.findAll(OrderStatus.PENDING, 0, 10);

        assertEquals(1, pendingOrders.size());
        assertEquals(order1.getId(), pendingOrders.get(0).getId());
    }

    @Test
    void testFindAll_WithoutStatus() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));

        repository.save(order1);
        repository.save(order2);

        List<Order> allOrders = repository.findAll(null, 0, 10);

        assertEquals(2, allOrders.size());
    }

    @Test
    void testFindByStatus() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order1.setStatus(OrderStatus.PENDING);
        
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));
        order2.setStatus(OrderStatus.PROCESSING);

        repository.save(order1);
        repository.save(order2);

        List<Order> pendingOrders = repository.findByStatus(OrderStatus.PENDING, 0, 10);

        assertEquals(1, pendingOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
    }

    @Test
    void testCount_WithStatus() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        order1.setStatus(OrderStatus.PENDING);
        
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));
        order2.setStatus(OrderStatus.PROCESSING);

        repository.save(order1);
        repository.save(order2);

        long pendingCount = repository.count(OrderStatus.PENDING);
        long processingCount = repository.count(OrderStatus.PROCESSING);

        assertEquals(1, pendingCount);
        assertEquals(1, processingCount);
    }

    @Test
    void testCount_WithoutStatus() {
        Order order1 = Order.create("customer-1", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));
        Order order2 = Order.create("customer-2", 
            Arrays.asList(new OrderItem("item-2", "Product", 1, BigDecimal.ONE)));

        repository.save(order1);
        repository.save(order2);

        long totalCount = repository.count(null);

        assertEquals(2, totalCount);
    }

    @Test
    void testDelete() {
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)));

        repository.save(order);
        assertTrue(repository.findById(order.getId()).isPresent());

        repository.delete(order.getId());

        assertFalse(repository.findById(order.getId()).isPresent());
    }

    @Test
    void testPagination() {
        // Create multiple orders
        for (int i = 0; i < 5; i++) {
            Order order = Order.create("customer-" + i, 
                Arrays.asList(new OrderItem("item-" + i, "Product", 1, BigDecimal.ONE)));
            repository.save(order);
        }

        List<Order> firstPage = repository.findAll(null, 0, 2);
        List<Order> secondPage = repository.findAll(null, 1, 2);
        List<Order> thirdPage = repository.findAll(null, 2, 2);

        assertEquals(2, firstPage.size());
        assertEquals(2, secondPage.size());
        assertEquals(1, thirdPage.size());
    }

    @Test
    void testIdempotencyIndex_Persistence() {
        String idempotencyKey = "key-123";
        Order order = Order.create("customer-123", 
            Arrays.asList(new OrderItem("item-1", "Product", 1, BigDecimal.ONE)), 
            idempotencyKey);

        repository.save(order);

        // Create new repository instance to test index loading
        FileOrderRepository newRepository = new FileOrderRepository(tempDir);
        Optional<Order> found = newRepository.findByIdempotencyKey(idempotencyKey);

        assertTrue(found.isPresent());
        assertEquals(idempotencyKey, found.get().getIdempotencyKey());
    }
}

