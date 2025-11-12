# Order Processing System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

 Order processing system built with Java 21 and Spring Boot 3.2.0. This system provides a RESTful API for managing orders with state machine validation, optimistic locking, pagination, and background processing capabilities.

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Running with Docker](#-running-with-docker)
- [Development](#-development)
- [Project Structure](#-project-structure)
- [State Machine](#-state-machine)
- [Monitoring & Health Checks](#-monitoring--health-checks)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

### Core Functionality
- ✅ **Order Management**: Create, retrieve, update, and cancel orders
- ✅ **State Machine**: Enforced order status transitions (PENDING → PROCESSING → SHIPPED → DELIVERED)
- ✅ **Optimistic Locking**: Version-based concurrency control to prevent data conflicts
- ✅ **Pagination**: Efficient pagination with status filtering
- ✅ **Background Processing**: Automatic transition of PENDING orders to PROCESSING status

### Technical Features
- 🔒 **File-based Persistence**: Atomic writes with JSON storage
- 📊 **Monitoring**: Prometheus metrics and Spring Boot Actuator
- 📚 **API Documentation**: Interactive Swagger/OpenAPI UI
- 🐳 **Docker Support**: Containerized deployment with Docker Compose
- 📝 **Structured Logging**: Comprehensive logging with SLF4J
- ⚡ **Performance**: Efficient file-based repository with lock management

## 🏗️ Architecture

```
┌─────────────────┐
│   REST API      │
│  (Controller)   │
└────────┬────────┘
         │
┌────────▼────────┐
│  Order Service  │
│  (Business      │
│   Logic)        │
└────────┬────────┘
         │
┌────────▼────────┐      ┌──────────────┐
│   Repository    │◄─────►│ State Machine│
│   Interface     │      │  (Validation)│
└────────┬────────┘      └──────────────┘
         │
┌────────▼────────┐
│ File Repository │
│  (JSON Files)   │
└─────────────────┘
```

### Components

- **Controller Layer**: REST endpoints with validation and error handling
- **Service Layer**: Business logic and state machine validation
- **Repository Layer**: File-based persistence with atomic operations
- **Scheduler**: Background job for automatic status transitions
- **State Machine**: Enforces valid order status transitions

## 📦 Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.6+ (for building)
- **Docker**: 20.10+ (optional, for containerized deployment)

## 🚀 Quick Start

### Option 1: Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd order-processing
   ```

2. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run the application**
   ```bash
   java -jar target/order-processing-0.0.1-SNAPSHOT.jar
   ```

4. **Access the application**
   - API: http://localhost:8080/api/v1/orders
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

### Option 2: Docker (Recommended)

1. **Using Docker Compose**
   ```bash
   docker-compose up -d
   ```

2. **View logs**
   ```bash
   docker-compose logs -f
   ```

3. **Stop the application**
   ```bash
   docker-compose down
   ```

## ⚙️ Configuration

Configuration is managed through `application.yml`. Key settings:

```yaml
server:
  port: 8080

order:
  repository:
    data-dir: data/orders  # Directory for storing order JSON files
  scheduler:
    delay: 300000  # Scheduler delay in milliseconds (5 minutes)

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

### Environment Variables

You can override configuration using environment variables:

```bash
export ORDER_REPOSITORY_DATA_DIR=/custom/path/orders
export ORDER_SCHEDULER_DELAY=600000
```

## 📚 API Documentation

### Interactive Documentation

Once the application is running, access the Swagger UI at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### API Endpoints

#### Create Order (Idempotent)
```http
POST /api/v1/orders
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{
  "customerId": "customer-123",
  "items": [
    {
      "itemId": "item-1",
      "name": "Product A",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

**Idempotency**: Include the `Idempotency-Key` header to ensure the same order isn't created twice. If an order with the same key already exists, the existing order is returned (HTTP 200). Otherwise, a new order is created (HTTP 201).

#### Get Order by ID
```http
GET /api/v1/orders/{id}
```

#### List Orders (with pagination)
```http
GET /api/v1/orders?status=PENDING&page=0&size=20
```

#### Update Order Status (Idempotent)
```http
PUT /api/v1/orders/{id}/status
Content-Type: application/json

{
  "status": "PROCESSING",
  "version": 1
}
```

**Idempotency**: If the order is already in the target status, the request succeeds without error and returns the existing order.

#### Cancel Order (Idempotent)
```http
DELETE /api/v1/orders/{id}?version=1
```

**Idempotency**: If the order is already cancelled, the request succeeds without error and returns the existing order.

### Response Examples

**Order Object:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "customerId": "customer-123",
  "status": "PENDING",
  "items": [
    {
      "itemId": "item-1",
      "name": "Product A",
      "quantity": 2,
      "price": 29.99,
      "status": "PENDING"
    }
  ],
  "totalAmount": 59.98,
  "version": 1,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Idempotency

All write operations (POST, PUT, DELETE) support idempotency:

- **POST /api/v1/orders**: Use `Idempotency-Key` header to prevent duplicate order creation
- **PUT /api/v1/orders/{id}/status**: Automatically idempotent - retrying with the same status returns the existing order
- **DELETE /api/v1/orders/{id}**: Automatically idempotent - retrying cancellation returns the existing order

**Best Practice**: Always include an `Idempotency-Key` header when creating orders to handle network retries safely.

## 🐳 Running with Docker

### Build Docker Image

```bash
docker build -t order-processing:latest .
```

### Run Container

```bash
docker run -d \
  --name order-processing \
  -p 8080:8080 \
  -v $(pwd)/data/orders:/app/data/orders \
  order-processing:latest
```

### Docker Compose

The included `docker-compose.yml` provides a complete setup:

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f order-processing

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

## 💻 Development

### Building from Source

```bash
# Compile and package
mvn clean package

# Run tests
mvn test

# Skip tests during build
mvn clean package -DskipTests
```

### Running Tests

```bash
mvn test
```

### IDE Setup

1. Import as Maven project in your IDE
2. Ensure JDK 21 is configured
3. Run `OrderProcessingApplication` main class

### Code Style

The project follows standard Java conventions and Spring Boot best practices.

## 📁 Project Structure

```
order-processing/
├── src/
│   ├── main/
│   │   ├── java/com/example/order/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── model/           # Domain models
│   │   │   ├── repository/      # Repository interfaces & implementations
│   │   │   ├── scheduler/       # Background jobs
│   │   │   ├── service/         # Business logic
│   │   │   └── state/           # State machine
│   │   └── resources/
│   │       └── application.yml  # Configuration
│   └── test/                    # Test files
├── data/orders/                 # Order data files (created at runtime)
├── Dockerfile                   # Docker image definition
├── docker-compose.yml           # Docker Compose configuration
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

## 🔄 State Machine

The order state machine enforces the following transitions:

```
PENDING → PROCESSING → SHIPPED → DELIVERED
   ↓
CANCELLED (only from PENDING)
```

**Valid Transitions:**
- `PENDING` → `PROCESSING` or `CANCELLED`
- `PROCESSING` → `SHIPPED`
- `SHIPPED` → `DELIVERED`
- `DELIVERED` → (terminal state)
- `CANCELLED` → (terminal state)

Invalid transitions will result in a `409 Conflict` response.

## 📊 Monitoring & Health Checks

### Actuator Endpoints

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Health Check Response

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

## 🔧 Troubleshooting

### Common Issues

**Issue: Port 8080 already in use**
```bash
# Change port in application.yml
server:
  port: 8081
```

**Issue: Data directory permissions**
```bash
# Ensure directory is writable
chmod 755 data/orders
```

**Issue: Docker container won't start**
```bash
# Check logs
docker-compose logs order-processing

# Verify Docker is running
docker ps
```

**Issue: Swagger UI not accessible**
- Verify the application is running
- Check that `springdoc.swagger-ui.enabled=true` in configuration
- Access via: http://localhost:8080/swagger-ui.html

### Logs

Application logs are written to the console. For Docker:

```bash
docker-compose logs -f order-processing
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Java coding conventions
- Add tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Order Processing Team** - *Initial work*

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- SpringDoc team for OpenAPI integration
- All contributors and users of this project

---

**Note**: This is a demo/prototype system. For production use, consider:
- Database persistence (PostgreSQL, MongoDB, etc.)
- Authentication and authorization
- Distributed tracing
- Enhanced monitoring and alerting
- Rate limiting and API security
