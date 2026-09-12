# High-Level Design (HLD): Order Service

**Version:** 1.0
**Status:** In Progress
**Author:** Senior Software Engineer

---

### 1. Overview & Scope

The **Order Service** is a core microservice responsible for a single function: capturing and persisting new customer orders.

**In Scope:**
* Exposing a REST API to accept new order requests.
* Validating the basic structure of an order request.
* Generating a unique order number.
* Persisting the order details to a relational database.

**Out of Scope:**
* Inventory Management (This will be handled by an `Inventory Service`).
* Payment Processing (This will be handled by a `Payment Service`).
* User Authentication/Management.
* Shipping and Fulfillment.

---

### 2. Architecture & Components

The service follows a standard 3-layer architecture to separate concerns.


+-----------+       +----------------+       +--------------------+       +----------------+
|           |       |                |       |                    |       |                |
|  Client   |------>| OrderController|------>|    OrderService    |------>| OrderRepository|
| (Postman) |       | (API Layer)    |       | (Business Logic)   |       | (Data Access)  |
|           |       |                |       |                    |       |                |
+-----------+       +----------------+       +--------------------+       +----------------+
|
|
v
+----------------+
|                |
|  MySQL DB      |
| (t_orders)     |
|                |
+----------------+


* **OrderController (`@RestController`):** The entry point. Handles incoming HTTP requests, deserializes the JSON payload into a DTO, and delegates to the `OrderService`.
* **OrderService (`@Service`):** Contains the core business logic. It maps the DTO to the `Order` entity, generates a unique `orderNumber`, and instructs the repository to save the order.
* **OrderRepository (`@Repository`):** An interface extending `JpaRepository`. It is responsible for all database CRUD (Create, Read, Update, Delete) operations for the `Order` entity.

---

### 3. Data Model / Schema

The service will persist data in a single table within the `order_service` database.

**Table: `t_orders`**

| Column Name   | Data Type     | Constraints              | Description                               |
|---------------|---------------|--------------------------|-------------------------------------------|
| `id`          | `BIGINT`      | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique identifier for the record.         |
| `order_number`| `VARCHAR(255)`| `NOT NULL`, `UNIQUE`     | A system-generated unique order number.   |
| `sku_code`    | `VARCHAR(255)`| `NOT NULL`               | The stock-keeping unit of the product.    |
| `price`       | `DECIMAL(19,2)`| `NOT NULL`               | The price of a single item at purchase.   |
| `quantity`    | `INT`         | `NOT NULL`               | The number of items ordered.              |

---

### 4. API Contract

The service exposes one primary endpoint.

**Endpoint:** `POST /api/order`

* **Description:** Creates a new order.
* **Request Body (`application/json`):**

    ```json
    {
      "skuCode": "iphone_15_pro",
      "price": 1299.99,
      "quantity": 1
    }
    ```

* **Success Response (Code `201 Created`):**

    ```text
    Order Placed Successfully
    ```

* **Error Response (Code `4xx/5xx`):**
    * A standard Spring Boot error response will be returned for bad requests or server errors.

---

### 5. Technology Stack

| Component              | Technology/Library | Justification                                       |
|------------------------|--------------------|-----------------------------------------------------|
| Language               | Java (21)          | Robust, enterprise-standard language.               |
| Framework              | Spring Boot (3.2)  | Rapid development of production-ready applications. |
| Web Server             | Tomcat (Embedded)  | Default for Spring Boot, reliable and performant.   |
| Database               | MySQL 8.3          | Industry-standard relational database.              |
| Data Access            | Spring Data JPA    | Simplifies database interaction and reduces code.   |
| Schema Migration       | Flyway             | For version-controlled, automated database changes. |
| Development Environment| Docker Compose     | Ensures consistent, reproducible environments.      |
