# Order Processing System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

RESTful API for managing orders with state machine validation, optimistic locking, pagination, and file-based persistence.

## Features

- **Order Management**: Create, retrieve, update, and cancel orders
- **State Machine**: Enforced forward-only transitions (PENDING → PROCESSING → SHIPPED → DELIVERED)
- **Optimistic Locking**: Version-based concurrency control
- **Idempotency**: Safe retries with idempotency keys
- **Pagination**: Efficient pagination with status filtering
- **Background Processing**: Automatic PENDING → PROCESSING transitions
- **Monitoring**: Prometheus metrics and Actuator endpoints
- **API Documentation**: Interactive Swagger/OpenAPI UI

## Prerequisites

- JDK 21+
- Maven 3.6+
- Docker 20.10+ (optional)

## Quick Start

### Local Development

```bash
# Clone and build
git clone <repository-url>
cd order-processing
mvn clean package -DskipTests

# Run
java -jar target/order-processing-0.0.1-SNAPSHOT.jar
```

**Access:**
- API: http://localhost:8080/api/v1/orders
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Docker

```bash
docker-compose up -d
docker-compose logs -f
```

## API Examples

### Create Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "customerId": "customer-123",
    "items": [{
      "itemId": "item-1",
      "name": "Product A",
      "quantity": 2,
      "price": 29.99
    }]
  }'
```

### Get Orders
```bash
# Get all orders (paginated)
curl http://localhost:8080/api/v1/orders?page=0&size=20

# Filter by status
curl http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=20

# Get by ID
curl http://localhost:8080/api/v1/orders/{id}
```

### Update Status
```bash
curl -X PUT http://localhost:8080/api/v1/orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PROCESSING", "version": 1}'
```

### Cancel Order
```bash
curl -X DELETE http://localhost:8080/api/v1/orders/{id}?version=1
```

## State Machine

```
PENDING → PROCESSING → SHIPPED → DELIVERED
   ↓
CANCELLED (only from PENDING)
```

- Forward-only transitions (no reverse)
- Terminal states: `DELIVERED`, `CANCELLED`
- Invalid transitions return `409 Conflict`

## Flow

1. **Order Creation**: Orders are created with `PENDING` status
   - Idempotency key prevents duplicate orders
   - Version starts at 1

2. **Automatic Processing**: Scheduler moves `PENDING` → `PROCESSING` every 5 minutes
   - Background job processes all pending orders
   - Uses optimistic locking (version check)

3. **Manual Updates**: Status can be updated via API
   - `PROCESSING` → `SHIPPED` → `DELIVERED`
   - Requires current version number
   - Idempotent (same status update returns existing order)

4. **Cancellation**: Orders can be cancelled from `PENDING` only
   - Requires version number
   - Terminal state (cannot be reactivated)

## Configuration

Key settings in `application.yml`:

```yaml
server:
  port: 8080

order:
  repository:
    data-dir: data/orders
  scheduler:
    delay: 300000  # 5 minutes

springdoc:
  swagger-ui:
    enabled: true
```

## Project Structure

```
order-processing/
├── src/main/java/com/example/order/
│   ├── config/          # Configuration
│   ├── controller/      # REST endpoints
│   ├── dto/             # Request/Response DTOs
│   ├── model/           # Domain models
│   ├── repository/       # File-based persistence
│   ├── scheduler/        # Background jobs
│   ├── service/          # Business logic
│   └── state/            # State machine
├── src/main/resources/
│   └── application.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Development

```bash
# Build
mvn clean package

# Test
mvn test

# Run from IDE
# Set JDK 21, run OrderProcessingApplication
```

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Troubleshooting

**Port 8080 in use**: Change `server.port` in `application.yml`

**Data directory**: Ensure `data/orders` is writable

**Swagger UI**: Verify `springdoc.swagger-ui.enabled=true`

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

**Guidelines:**
- Follow Java conventions
- Add tests for new features
- Update documentation
- Ensure tests pass

## License

Apache License 2.0 - see [LICENSE](LICENSE) file for details.

---

**Note**: This is a demo/prototype. For production, consider:
- Database persistence (PostgreSQL, MongoDB)
- Authentication and authorization
- Enhanced monitoring and alerting
- Rate limiting and API security
