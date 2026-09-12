# E-Commerce Microservices Platform - Design Documentation

## Table of Contents
- [Project Overview](#project-overview)
- [High-Level Design (HLD)](#high-level-design-hld)
- [Low-Level Design (LLD)](#low-level-design-lld)
- [Deployment Architecture](#deployment-architecture)
- [Frontend Architecture](#frontend-architecture)
- [Future Roadmap](#future-roadmap)

## Project Overview

**Project Name:** E-Commerce Microservices Platform  
**Version:** 1.0.0  
**Architecture Pattern:** Event-Driven Microservices  
**Tech Stack:** Java, Spring Boot, Spring Cloud, Apache Kafka, React

### System Goals
- Build a scalable, resilient, and production-ready e-commerce platform
- Demonstrate advanced backend engineering skills through distributed systems
- Implement event-driven architecture for loose coupling and high availability
- Provide comprehensive observability and security features

---

## High-Level Design (HLD)

### System Architecture Overview

The system follows an event-driven microservices architecture with clear separation of concerns between business logic, infrastructure services, and cross-cutting concerns.

### Core Business Services

#### 1. Product Service
- **Purpose:** Manages the complete product catalog
- **Technology:** Spring Boot + MongoDB
- **Port:** 8083
- **Responsibilities:**
    - Product CRUD operations
    - Product metadata management
    - Product search and filtering
- **Key Dependencies:**
    - `spring-boot-starter-data-mongodb`
    - `spring-boot-starter-web`

#### 2. Inventory Service
- **Purpose:** Manages stock levels and inventory operations
- **Technology:** Spring Boot + MySQL
- **Port:** 8082
- **Responsibilities:**
    - Stock level management
    - Atomic stock reduction operations
    - Inventory validation for orders
- **Key Dependencies:**
    - `spring-boot-starter-data-jpa`
    - `flyway-mysql`

#### 3. Order Service
- **Purpose:** Manages the complete order lifecycle
- **Technology:** Spring Boot + MySQL + Kafka
- **Port:** 8080
- **Responsibilities:**
    - Order creation and management
    - Stock verification coordination
    - Event publishing for order states
- **Key Dependencies:**
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-webflux`
    - `spring-kafka`

#### 4. Notification Service (Planned)
- **Purpose:** Handles asynchronous user notifications
- **Technology:** Spring Boot + Kafka
- **Port:** 8084
- **Status:** Planned for future implementation
- **Responsibilities:**
    - Email notifications
    - SMS notifications
    - Push notifications

### Infrastructure Services

#### 1. Discovery Server (Eureka)
- **Technology:** Spring Cloud Netflix Eureka
- **Port:** 8761
- **Role:** Service registry for dynamic service discovery
- **Features:**
    - Automatic service registration
    - Health checking
    - Load balancing support

#### 2. API Gateway
- **Technology:** Spring Cloud Gateway
- **Port:** 8181
- **Role:** Single entry point for all client traffic
- **Features:**
    - Request routing
    - Load balancing
    - Security enforcement
    - Rate limiting
    - Request/response transformation

#### 3. Message Broker (Kafka)
- **Technology:** Apache Kafka
- **Port:** 9092
- **Role:** Durable event bus for asynchronous communication
- **Features:**
    - Event streaming
    - Message persistence
    - Scalable message processing

#### 4. Config Server (Planned)
- **Technology:** Spring Cloud Config
- **Port:** 8888
- **Role:** Centralized configuration management
- **Status:** Planned for future implementation

#### 5. Auth Server (Planned)
- **Technology:** Keycloak
- **Port:** 8085
- **Role:** Identity and Access Management (IAM)
- **Status:** Planned for future implementation

#### 6. Tracing Server (Planned)
- **Technology:** Zipkin
- **Port:** 9411
- **Role:** Distributed tracing for observability
- **Status:** Planned for future implementation

### Communication Patterns

#### Synchronous Communication
```
Client → API Gateway → Order Service → Inventory Service
```
- Used for immediate response requirements
- REST API with WebClient
- Circuit breaker pattern for resilience

#### Asynchronous Communication
```
Order Service → Kafka Topic → Notification Service
```
- Used for background processing
- Event-driven architecture
- Eventual consistency model

### Cross-Cutting Concerns

#### 1. Security (Planned)
- **Technologies:** Spring Security, OAuth2/OIDC, JWT, Keycloak
- **Implementation:** API Gateway as OAuth2 Resource Server
- **Features:**
    - JWT token validation
    - Role-based access control
    - Secure inter-service communication

#### 2. Resilience (Planned)
- **Technology:** Resilience4j
- **Implementation:** Circuit Breaker on Order Service → Inventory Service calls
- **Features:**
    - Fault tolerance
    - Cascading failure prevention
    - Graceful degradation

#### 3. Observability (Planned)
- **Technologies:** Micrometer, Zipkin
- **Implementation:** Distributed tracing across all services
- **Features:**
    - Request tracing
    - Performance monitoring
    - Service dependency mapping

---

## Low-Level Design (LLD)

### API Gateway Details

#### Configuration
```java
@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("product-service", r -> r.path("/api/product/**")
                .uri("lb://product-service"))
            .route("order-service", r -> r.path("/api/order/**")
                .uri("lb://order-service"))
            .route("inventory-service", r -> r.path("/api/inventory/**")
                .uri("lb://inventory-service"))
            .build();
    }
}
```

#### Route Definitions
| Route ID | Path Pattern | Destination | Load Balancer |
|----------|-------------|-------------|---------------|
| product-service | `/api/product/**` | `lb://product-service` | Yes |
| order-service | `/api/order/**` | `lb://order-service` | Yes |
| inventory-service | `/api/inventory/**` | `lb://inventory-service` | Yes |

### Order Service Details

#### API Contract
**Endpoint:** `POST /api/order`

**Request Body:**
```json
{
  "orderLineItems": [
    {
      "skuCode": "string",
      "price": "decimal",
      "quantity": "integer"
    }
  ]
}
```

**Success Response:**
- **Status Code:** 201 Created
- **Description:** Order successfully created

#### Business Logic Flow
1. **Request Reception:** Receive and validate OrderRequest
2. **Inventory Preparation:** Construct List<InventoryRequest> for stock verification
3. **Stock Verification:** Call Inventory Service's `POST /api/inventory/check` using WebClient with Circuit Breaker
4. **Order Persistence:** Save Order to MySQL database within transaction
5. **Event Publishing:** Publish OrderPlacedEvent to Kafka 'notificationTopic'
6. **Response:** Return 201 Created status to client

#### Data Models

**Order Entity:**
```java
@Entity
@Table(name = "t_orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderNumber;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderLineItems> orderLineItemsList;
    
    private LocalDateTime createdDate;
    private OrderStatus status;
}
```

**OrderLineItems Entity:**
```java
@Entity
@Table(name = "t_order_line_items")
public class OrderLineItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String skuCode;
    private BigDecimal price;
    private Integer quantity;
}
```

#### Event Schema
**OrderPlacedEvent:**
```java
public class OrderPlacedEvent {
    private String orderNumber;
    private String customerEmail;
    private List<OrderLineItemsDto> orderLineItems;
    private LocalDateTime orderDate;
}
```

### Inventory Service Details

#### API Contract

**Endpoint 1:** `GET /api/inventory`
- **Purpose:** Check stock availability
- **Parameters:** `skuCode` (query parameter, can be multiple)
- **Response:** List of inventory status

**Endpoint 2:** `POST /api/inventory/check`
- **Purpose:** Atomic stock check and reservation
- **Request Body:**
```json
{
  "inventoryRequests": [
    {
      "skuCode": "string",
      "quantity": "integer"
    }
  ]
}
```
- **Response:** Boolean indicating stock availability and reservation success

#### Database Schema
**Inventory Table:**
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code VARCHAR(255) NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Product Service Details

#### API Contract

**Endpoints:**
- `GET /api/product` - Retrieve all products
- `GET /api/product/{id}` - Retrieve specific product
- `POST /api/product` - Create new product
- `PUT /api/product/{id}` - Update existing product
- `DELETE /api/product/{id}` - Delete product

#### Document Schema (MongoDB)
```javascript
{
  "_id": "ObjectId",
  "name": "String",
  "description": "String",
  "price": "Decimal128",
  "skuCode": "String", // Unique identifier
  "category": "String",
  "imageUrls": ["String"],
  "specifications": {
    "key": "value"
  },
  "createdDate": "Date",
  "updatedDate": "Date",
  "isActive": "Boolean"
}
```

### Inter-Service Communication

#### WebClient Configuration
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

#### Circuit Breaker Configuration
```java
@Component
public class InventoryServiceClient {
    
    @Retryable(value = {Exception.class}, maxAttempts = 3)
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackMethod")
    public Boolean checkInventory(List<InventoryRequest> requests) {
        // WebClient call to inventory service
    }
    
    public Boolean fallbackMethod(List<InventoryRequest> requests, Exception ex) {
        // Fallback logic when inventory service is down
        return false;
    }
}
```

---

## Deployment Architecture

### Containerization Strategy

#### Docker Configuration
Each microservice will have its dedicated Dockerfile following multi-stage build pattern:

```dockerfile
# Example Dockerfile for Spring Boot services
FROM openjdk:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Local Development

#### Docker Compose Setup
```yaml
version: '3.8'
services:
  # Infrastructure Services
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
  
  api-gateway:
    build: ./api-gateway
    ports:
      - "8181:8181"
    depends_on:
      - eureka-server
  
  # Databases
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ecommerce
    ports:
      - "3306:3306"
  
  mongodb:
    image: mongo:5.0
    ports:
      - "27017:27017"
  
  # Message Broker
  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
  
  # Business Services
  product-service:
    build: ./product-service
    ports:
      - "8083:8083"
    depends_on:
      - mongodb
      - eureka-server
  
  inventory-service:
    build: ./inventory-service
    ports:
      - "8082:8082"
    depends_on:
      - mysql
      - eureka-server
  
  order-service:
    build: ./order-service
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - kafka
      - eureka-server
```

### Production Deployment (Kubernetes)

#### Service Deployment Example
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: order-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
          value: "http://eureka-service:8761/eureka"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

---

## Frontend Architecture

### React SPA Structure

#### Technology Stack
- **Framework:** React 18+
- **State Management:** Redux Toolkit or Context API
- **Routing:** React Router v6
- **HTTP Client:** Axios
- **UI Framework:** Material-UI or Tailwind CSS

#### Communication Pattern
```
React Frontend ↔ API Gateway (Port 8181) ↔ Microservices
```

#### Key Components
- **Product Catalog:** Browse and search products
- **Shopping Cart:** Manage cart items
- **Checkout:** Place orders through Order Service
- **Order History:** View past orders
- **User Authentication:** Login/Register (when auth service is implemented)

#### API Integration Example
```javascript
// API service layer
class ApiService {
  static baseURL = 'http://localhost:8181/api';
  
  static async getProducts() {
    const response = await axios.get(`${this.baseURL}/product`);
    return response.data;
  }
  
  static async placeOrder(orderData) {
    const response = await axios.post(`${this.baseURL}/order`, orderData);
    return response.data;
  }
}
```

---

## Future Roadmap

### Phase 2 - Security & Configuration
- **Config Server Implementation:** Centralized configuration management
- **Keycloak Integration:** Complete authentication and authorization
- **JWT Security:** End-to-end security implementation
- **API Rate Limiting:** Protection against abuse

### Phase 3 - Observability & Monitoring
- **Zipkin Integration:** Distributed tracing
- **Metrics Collection:** Prometheus + Grafana
- **Centralized Logging:** ELK Stack
- **Health Checks:** Custom actuator endpoints

### Phase 4 - Advanced Features
- **Notification Service:** Email/SMS notifications
- **Payment Service:** Payment processing integration
- **Recommendation Engine:** AI-powered product recommendations
- **Caching Layer:** Redis for performance optimization

### Phase 5 - DevOps & Automation
- **CI/CD Pipelines:** GitHub Actions or Jenkins
- **Infrastructure as Code:** Terraform for cloud deployment
- **Auto-scaling:** Kubernetes HPA and VPA
- **Disaster Recovery:** Multi-region deployment

---

## Conclusion

This design document provides a comprehensive blueprint for building a production-ready e-commerce platform using modern microservices architecture. The system is designed with scalability, resilience, and maintainability as core principles, making it suitable for both learning purposes and real-world applications.

The modular design allows for incremental development and deployment, with clear separation of concerns between business logic, infrastructure services, and cross-cutting concerns. The planned features ensure the system can evolve to meet complex enterprise requirements while maintaining high availability and performance standards.