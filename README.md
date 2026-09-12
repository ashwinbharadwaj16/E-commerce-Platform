# 🛒 E-Commerce Microservices Platform

A **Java Spring Boot microservices-based e-commerce platform** designed to demonstrate distributed systems, service-to-service communication, event-driven architecture, database separation, API gateway routing, and containerized deployment.

The platform is built around independent services for **products, inventory, and orders**, with **Apache Kafka** used for asynchronous event communication and **Netflix Eureka** used for service discovery.

---

## 🏗️ Architecture

The application follows a **microservices architecture** where each business capability is implemented as an independently deployable service.

```text
                         ┌───────────────────┐
                         │      Client       │
                         └─────────┬─────────┘
                                   │
                                   ▼
                         ┌───────────────────┐
                         │    API Gateway    │
                         │      :8181        │
                         └─────────┬─────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
      ┌───────────────┐    ┌───────────────┐    ┌───────────────┐
      │ Product       │    │ Order         │    │ Inventory     │
      │ Service       │    │ Service       │    │ Service       │
      │ :8083         │    │ :8080         │    │ :8082         │
      └───────┬───────┘    └───────┬───────┘    └───────┬───────┘
              │                    │                    │
              ▼                    ▼                    ▼
         ┌─────────┐          ┌─────────┐          ┌─────────┐
         │ MongoDB │          │  MySQL  │          │  MySQL  │
         └─────────┘          └─────────┘          └─────────┘
                                   │
                                   ▼
                              ┌─────────┐
                              │  Kafka  │
                              └─────────┘

                         ┌───────────────────┐
                         │   Eureka Server   │
                         │      :8761        │
                         └───────────────────┘
```

### Request Flow

```text
Client
  ↓
API Gateway
  ↓
Eureka Service Discovery
  ↓
Business Microservice
  ↓
Service-specific Database
```

### Order Flow

```text
Client
  ↓
API Gateway
  ↓
Order Service
  ↓
Inventory Service
  ↓
Order persisted in MySQL
  ↓
Order event published to Kafka
```

The detailed architecture and design documentation is available in [`ARCHITECTURE.md`](./ARCHITECTURE.md).

---

# ✨ Features

### 🏪 Product Management

* Create products
* Retrieve all products
* Retrieve product by ID
* Update product details
* Delete products
* Product persistence using MongoDB

### 📦 Inventory Management

* Track product stock
* Check inventory availability
* Validate requested quantities
* Maintain inventory independently from the Order Service
* MySQL-based persistence

### 🛍️ Order Management

* Create orders
* Manage order line items
* Validate inventory before placing an order
* Persist orders using MySQL
* Generate order numbers
* Publish order events asynchronously through Kafka

### 🌐 API Gateway

* Single entry point for client requests
* Routes requests to individual microservices
* Service-to-service routing through Eureka
* Load-balanced service discovery

### 🔍 Service Discovery

* Netflix Eureka-based service registry
* Dynamic service registration
* Service discovery between microservices
* Load-balanced communication

### 📨 Event-Driven Communication

Apache Kafka is used for asynchronous communication.

```text
Order Service
     │
     │ OrderPlacedEvent
     ▼
   Kafka
     │
     ▼
Consumers / Notification Workflow
```

### 🐳 Containerized Infrastructure

Docker Compose is used to run supporting infrastructure including:

* MySQL
* MongoDB
* Apache Kafka
* Zookeeper
* Keycloak

---

# 🧰 Technology Stack

| Category                      | Technology                  |
| ----------------------------- | --------------------------- |
| Language                      | Java                        |
| Framework                     | Spring Boot                 |
| Microservices                 | Spring Cloud                |
| API Gateway                   | Spring Cloud Gateway        |
| Service Discovery             | Netflix Eureka              |
| Messaging                     | Apache Kafka                |
| Relational Database           | MySQL                       |
| NoSQL Database                | MongoDB                     |
| ORM                           | Spring Data JPA / Hibernate |
| HTTP Client                   | Spring WebClient            |
| Containerization              | Docker                      |
| Authentication Infrastructure | Keycloak                    |
| Build Tool                    | Maven                       |

---

# 📦 Microservices

| Service           |   Port | Database | Responsibility    |
| ----------------- | -----: | -------- | ----------------- |
| API Gateway       | `8181` | —        | Request routing   |
| Eureka Server     | `8761` | —        | Service discovery |
| Product Service   | `8083` | MongoDB  | Product catalog   |
| Inventory Service | `8082` | MySQL    | Stock management  |
| Order Service     | `8080` | MySQL    | Order processing  |

### Product Service

Responsible for the product catalog.

**Database:** MongoDB

Main operations:

```text
GET     /api/product
GET     /api/product/{id}
POST    /api/product
PUT     /api/product/{id}
DELETE  /api/product/{id}
```

### Inventory Service

Responsible for stock management and availability validation.

**Database:** MySQL

Main operations:

```text
GET  /api/inventory
POST /api/inventory/check
```

### Order Service

Responsible for order creation and order processing.

**Database:** MySQL

Main operation:

```text
POST /api/order
```

During order creation, the service:

1. Receives the order request.
2. Validates the order details.
3. Checks inventory availability.
4. Persists the order.
5. Publishes an order event to Kafka.

---

# 🔄 Order Processing Flow

```text
                  Client
                    │
                    ▼
              API Gateway
                    │
                    ▼
              Order Service
                    │
                    ▼
           Inventory Service
                    │
             Stock Available?
                /       \
              No         Yes
              │           │
              ▼           ▼
        Reject Order   Save Order
                           │
                           ▼
                     Publish Event
                           │
                           ▼
                         Kafka
```

This design keeps inventory management separate from order management and allows asynchronous processing through Kafka.

---

# 📨 Kafka Event Flow

The Order Service publishes an `OrderPlacedEvent` after successfully creating an order.

Example event structure:

```json
{
  "orderNumber": "ORDER-12345",
  "customerEmail": "customer@example.com",
  "orderDate": "2026-09-12T13:30:00",
  "orderLineItems": [
    {
      "skuCode": "iphone-13",
      "price": 999.99,
      "quantity": 2
    }
  ]
}
```

Kafka provides asynchronous communication between the order workflow and downstream consumers.

---

# 🗄️ Database Strategy

The project follows a **database-per-service approach**.

### Product Service

```text
Product Service
      │
      ▼
   MongoDB
```

MongoDB is used for product data because product information can contain flexible attributes such as specifications and image URLs.

### Order Service

```text
Order Service
      │
      ▼
    MySQL
```

MySQL stores orders and order line items using a relational model.

### Inventory Service

```text
Inventory Service
      │
      ▼
    MySQL
```

Inventory data is maintained independently from the Order Service.

This separation reduces coupling between business domains and allows individual services to evolve independently.

---

# 🚀 Getting Started

## Prerequisites

Install the following:

* Java 17+
* Maven 3.6+
* Docker
* Docker Compose

Verify your installation:

```bash
java --version
mvn --version
docker --version
docker compose version
```

---

## 1. Clone the Repository

```bash
git clone https://github.com/ashwinbharadwaj16/E-commerce-Platform.git

cd E-commerce-Platform
```

---

## 2. Start Infrastructure

The Docker Compose configuration is located under the `docker` directory.

```bash
cd docker

docker compose up -d
```

This starts the supporting infrastructure:

```text
MySQL Order Database
MySQL Inventory Database
MongoDB Product Database
Apache Kafka
Zookeeper
Keycloak
```

---

## 3. Start the Microservices

Start the following applications using your IDE or Maven:

```text
Eureka Server
API Gateway
Product Service
Inventory Service
Order Service
```

Recommended startup order:

```text
1. Eureka Server
2. Product Service
3. Inventory Service
4. Order Service
5. API Gateway
```

Once the services are registered, verify them through the Eureka dashboard:

```text
http://localhost:8761
```

---

# 🌐 Service Endpoints

| Component         | URL                     |
| ----------------- | ----------------------- |
| API Gateway       | `http://localhost:8181` |
| Eureka Dashboard  | `http://localhost:8761` |
| Product Service   | `http://localhost:8083` |
| Inventory Service | `http://localhost:8082` |
| Order Service     | `http://localhost:8080` |
| Kafka             | `localhost:9092`        |
| MongoDB           | `localhost:27017`       |
| MySQL - Order     | `localhost:3306`        |
| MySQL - Inventory | `localhost:3307`        |
| Keycloak          | `http://localhost:9090` |

---

# 🧪 API Examples

## Create Product

```bash
curl -X POST http://localhost:8181/api/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 13",
    "description": "Apple iPhone 13",
    "price": 999.99,
    "skuCode": "iphone-13"
  }'
```

---

## Get Products

```bash
curl http://localhost:8181/api/product
```

---

## Check Inventory

```bash
curl "http://localhost:8181/api/inventory?skuCode=iphone-13"
```

---

## Place an Order

```bash
curl -X POST http://localhost:8181/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "orderLineItems": [
      {
        "skuCode": "iphone-13",
        "price": 999.99,
        "quantity": 2
      }
    ]
  }'
```

---

# 🧩 Project Structure

```text
E-commerce-Platform/
│
├── api-gateway/
│   └── API Gateway application
│
├── discovery_server/
│   └── Eureka Discovery Server
│
├── product_service/
│   └── Product catalog microservice
│
├── inventory_service/
│   └── Inventory management microservice
│
├── order_service/
│   └── Order processing microservice
│
├── notificaiton-service/
│   └── Notification-related components
│
├── docker/
│   └── Docker Compose infrastructure
│
├── ARCHITECTURE.md
│
└── README.md
```

---

# 🔗 Service Communication

The platform uses two primary communication patterns.

### Synchronous

Used when the calling service requires an immediate response.

```text
Order Service
      │
      │ WebClient
      ▼
Inventory Service
```

For example, the Order Service checks inventory before creating an order.

### Asynchronous

Used for event-based workflows.

```text
Order Service
      │
      ▼
    Kafka
      │
      ▼
Downstream Consumers
```

This allows downstream processing to be decoupled from the main order transaction.

---

# 🐳 Docker Infrastructure

The project uses Docker Compose for local infrastructure.

```bash
cd docker

docker compose up -d
```

Stop the infrastructure:

```bash
docker compose down
```

Stop and remove persistent volumes:

```bash
docker compose down -v
```

---

# 🔐 Security

Keycloak infrastructure is included in the Docker environment for authentication and authorization development.

The planned security architecture includes:

```text
Client
   │
   ▼
API Gateway
   │
   ▼
Keycloak
   │
   ▼
JWT Authentication
   │
   ▼
Microservices
```

Planned security capabilities include:

* OAuth2 / OpenID Connect
* JWT authentication
* Role-based authorization
* API Gateway security
* Secure inter-service communication

> Authentication and authorization are part of the project's ongoing development roadmap.

---

# 📈 Scalability & Resilience

The architecture is designed to support horizontal scaling of individual services.

For example:

```text
              API Gateway
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
    Order #1   Order #2   Order #3
        │          │          │
        └──────────┼──────────┘
                   ▼
                Kafka
```

Eureka enables service discovery while Spring Cloud Gateway provides centralized routing.

Future resilience improvements include:

* Circuit breakers
* Retry mechanisms
* Distributed tracing
* Centralized configuration
* Metrics and monitoring

---

# 🧪 Testing

The project can be tested at multiple levels:

### Unit Testing

Test individual controllers, services, and business logic independently.

### Integration Testing

Test communication between:

```text
Order Service
      ↓
Inventory Service
      ↓
Database
```

### API Testing

The REST APIs can be tested using:

* cURL
* Postman
* IntelliJ HTTP Client

---

# 🛣️ Roadmap

## Phase 1 — Core Microservices ✅

* [x] Product Service
* [x] Inventory Service
* [x] Order Service
* [x] API Gateway
* [x] Eureka Service Discovery
* [x] MySQL persistence
* [x] MongoDB persistence
* [x] Kafka integration
* [x] Docker infrastructure

## Phase 2 — Security 🔄

* [ ] Complete Keycloak integration
* [ ] JWT authentication
* [ ] Role-based authorization
* [ ] Secure API Gateway
* [ ] Centralized configuration

## Phase 3 — Resilience & Observability 🔄

* [ ] Resilience4j circuit breaker
* [ ] Distributed tracing
* [ ] Prometheus metrics
* [ ] Grafana dashboards
* [ ] Centralized logging

## Phase 4 — E-Commerce Features 📅

* [ ] Payment Service
* [ ] Notification Service
* [ ] Advanced product search
* [ ] Shopping cart
* [ ] User management
* [ ] Recommendation engine
* [ ] Redis caching

## Phase 5 — Deployment & Automation 📅

* [ ] Kubernetes deployment
* [ ] CI/CD pipeline
* [ ] Automated container builds
* [ ] Kubernetes autoscaling
* [ ] Cloud deployment

---

# 📚 Documentation

For a deeper explanation of the system design, see:

**[ARCHITECTURE.md](./ARCHITECTURE.md)**

The architecture document covers:

* High-Level Design
* Low-Level Design
* Service responsibilities
* API contracts
* Database models
* Kafka communication
* Docker architecture
* Deployment architecture
* Future improvements

---

# 🎯 What This Project Demonstrates

This project demonstrates practical implementation of:

* Java backend development
* Spring Boot
* Spring Cloud
* Microservices architecture
* REST APIs
* API Gateway
* Service Discovery
* Inter-service communication
* Apache Kafka
* Event-driven architecture
* MySQL
* MongoDB
* Spring Data JPA
* WebClient
* Docker
* Distributed system design

---

# 🤝 Contributing

Contributions and improvements are welcome.

```bash
git checkout -b feature/<feature-name>

git add .

git commit -m "Add <feature-name>"

git push origin feature/<feature-name>
```

Then open a Pull Request.

---

## 👨‍💻 Author

**Ashwin Bharadwaj**

Java Backend Developer | Spring Boot | Microservices | Kafka | Docker

