package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileOrderRepository implements OrderRepository {
    private static final Logger log = LoggerFactory.getLogger(FileOrderRepository.class);
    private static final int MAX_LOCKS = 10000; // Prevent unbounded growth
    private static final String FILE_PATTERN = "*.json";
    
    private final Path baseDir;
    private final Path idempotencyIndexFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>(); // In-memory cache

    public FileOrderRepository(Path baseDir) {
        this.baseDir = baseDir;
        this.idempotencyIndexFile = baseDir.resolve("_idempotency_index.json");
        try {
            Files.createDirectories(baseDir);
            loadIdempotencyIndex();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadIdempotencyIndex() {
        if (Files.exists(idempotencyIndexFile)) {
            try (InputStream is = Files.newInputStream(idempotencyIndexFile)) {
                @SuppressWarnings("unchecked")
                Map<String, String> index = mapper.readValue(is, Map.class);
                idempotencyIndex.putAll(index);
                log.debug("Loaded {} idempotency keys from index", index.size());
            } catch (IOException e) {
                log.warn("Failed to load idempotency index, starting fresh: {}", e.getMessage());
            }
        }
    }

    private void saveIdempotencyIndex() {
        try {
            Path tmp = baseDir.resolve("_idempotency_index.json.tmp");
            try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                mapper.writeValue(os, idempotencyIndex);
            }
            Files.move(tmp, idempotencyIndexFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save idempotency index: {}", e.getMessage(), e);
        }
    }

    private ReentrantReadWriteLock lockFor(String id) {
        // Cleanup old locks if map grows too large
        if (locks.size() > MAX_LOCKS) {
            synchronized (locks) {
                if (locks.size() > MAX_LOCKS) {
                    // Remove locks that are not currently in use
                    locks.entrySet().removeIf(entry -> {
                        ReentrantReadWriteLock lock = entry.getValue();
                        return !lock.isWriteLocked() && lock.getReadLockCount() == 0;
                    });
                    log.debug("Cleaned up locks, current size: {}", locks.size());
                }
            }
        }
        return locks.computeIfAbsent(id, k -> new ReentrantReadWriteLock());
    }

    private Path pathFor(String id) {
        return baseDir.resolve(id + ".json");
    }

    @Override
    public Order save(Order order) {
        String id = order.getId();
        ReentrantReadWriteLock l = lockFor(id);
        l.writeLock().lock();
        try {
            Path p = pathFor(id);
            // version increment
            order.setVersion(order.getVersion() + 1);
            order.setUpdatedAt(java.time.Instant.now());
            
            // Update idempotency index if key exists
            if (order.getIdempotencyKey() != null && !order.getIdempotencyKey().isBlank()) {
                idempotencyIndex.put(order.getIdempotencyKey(), id);
                saveIdempotencyIndex();
            }
            
            Path tmp = baseDir.resolve(id + ".json.tmp");
            try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                mapper.writeValue(os, order);
            }
            Files.move(tmp, p, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return order;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            l.writeLock().unlock();
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        Path p = pathFor(id);
        if (!Files.exists(p)) return Optional.empty();
        ReentrantReadWriteLock l = lockFor(id);
        l.readLock().lock();
        try (InputStream is = Files.newInputStream(p)) {
            Order o = mapper.readValue(is, Order.class);
            return Optional.of(o);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            l.readLock().unlock();
        }
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        
        // Use index for fast lookup instead of reading all orders
        String orderId = idempotencyIndex.get(idempotencyKey);
        if (orderId != null) {
            return findById(orderId);
        }
        
        return Optional.empty();
    }

    private List<Order> readAllOrders() {
        try {
            if (!Files.exists(baseDir)) return Collections.emptyList();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(baseDir, FILE_PATTERN)) {
                List<Order> out = new ArrayList<>();
                for (Path p : ds) {
                    // Skip temporary files
                    if (p.getFileName().toString().endsWith(".tmp")) {
                        continue;
                    }
                    try (InputStream is = Files.newInputStream(p)) {
                        Order o = mapper.readValue(is, Order.class);
                        out.add(o);
                    } catch (IOException e) {
                        log.warn("Failed to read order file {}: {}", p.getFileName(), e.getMessage());
                    }
                }
                return out;
            }
        } catch (IOException e) {
            log.error("Error reading orders from directory: {}", e.getMessage(), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<Order> findAll(OrderStatus status, int page, int size) {
        List<Order> all = readAllOrders();
        List<Order> filtered = all;
        if (status != null) {
            filtered = all.stream().filter(o -> o.getStatus() == status).collect(Collectors.toList());
        }
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int page, int size) {
        if (status == null) {
            return findAll(null, page, size);
        }
        return findAll(status, page, size);
    }

    @Override
    public long count(OrderStatus status) {
        List<Order> all = readAllOrders();
        if (status == null) {
            return all.size();
        }
        return all.stream().filter(o -> o.getStatus() == status).count();
    }

    @Override
    public void delete(String id) {
        Path p = pathFor(id);
        ReentrantReadWriteLock l = lockFor(id);
        l.writeLock().lock();
        try {
            // Remove from idempotency index if exists
            Optional<Order> order = findById(id);
            if (order.isPresent() && order.get().getIdempotencyKey() != null) {
                idempotencyIndex.remove(order.get().getIdempotencyKey());
                saveIdempotencyIndex();
            }
            
            if (Files.deleteIfExists(p)) {
                log.debug("Deleted order file: {}", id);
            }
            // Clean up lock after deletion
            locks.remove(id);
        } catch (IOException e) {
            log.error("Error deleting order file {}: {}", id, e.getMessage(), e);
            throw new UncheckedIOException(e);
        } finally {
            l.writeLock().unlock();
        }
    }
}
