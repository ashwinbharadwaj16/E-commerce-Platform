# Design Document: Inventory Service

**Version:** 1.0
**Status:** Ready for Implementation

---

## 1.0 High-Level Design (HLD)

### 1.1. Overview & Scope

The **Inventory Service** is a specialized microservice with a single responsibility: to manage and provide information about product stock levels.

**In Scope:**
* Exposing a REST API to check if a product is in stock.
* Storing inventory data, specifically the `skuCode` and the available `quantity`.
* (Future Scope) Providing an internal mechanism to update stock levels after an order is placed.

**Out of Scope:**
* Product details (name, description, price). This is the Product Service's job.
* Order processing. This is the Order Service's job.

### 1.2. Architecture

The service follows the same standard 3-layer architecture as our previous services.


+-----------------+      +---------------------+      +----------------------+      +-----------------+
|                 |      |                     |      |                      |      |                 |
|  Order Service  |----->| InventoryController |----->|   InventoryService   |----->| InventoryRepo   |
| (Client)        |      | (API Layer)         |      | (Business Logic)     |      | (Data Access)   |
|                 |      |                     |      |                      |      |                 |
+-----------------+      +---------------------+      +----------------------+      +-------+---------+
|
v
+-----------------+
|                 |
|  Inventory DB   |
|   (t_inventory) |
+-----------------+


### 1.3. API Contract

The service will expose one primary `GET` endpoint.

* **Endpoint:** `GET /api/inventory`
* **Description:** Checks the stock for one or more products.
* **Request:** The `skuCode`(s) will be passed as URL query parameters.
    * Example: `http://localhost:8082/api/inventory?skuCode=iphone_15_pro`
* **Success Response (Code `200 OK`):** An array of `InventoryResponse` objects.
    ```json
    [
      {
        "skuCode": "iphone_15_pro",
        "isInStock": true
      }
    ]
    ```

### 1.4. Data Model

* **Database:** `inventory_service` (MySQL)
* **Table:** `t_inventory`

| Column Name | Data Type      | Constraints              | Description                              |
|-------------|----------------|--------------------------|------------------------------------------|
| `id`        | `BIGINT`       | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique identifier for the record.        |
| `sku_code`  | `VARCHAR(255)` | `NOT NULL`, `UNIQUE`     | The stock-keeping unit of the product.   |
| `quantity`  | `INT`          | `NOT NULL`               | The number of items currently in stock. |

---

## 2.0 Low-Level Design (LLD)

### 2.1. Class Design

* **DTO: `InventoryResponse.java`**
    * **Purpose:** The data structure returned by the API.
    * **Fields:** `private String skuCode;`, `private boolean isInStock;`

* **Controller: `InventoryController.java`**
    * **Annotation:** `@RestController`, `@RequestMapping("/api/inventory")`
    * **Method:** `isInStock(@RequestParam List<String> skuCode)`
        * **Annotation:** `@GetMapping`, `@ResponseStatus(HttpStatus.OK)`
        * **Logic:** Receives a list of `skuCode`s and calls the `InventoryService`.

* **Service: `InventoryService.java`**
    * **Annotation:** `@Service`
    * **Method:** `isInStock(List<String> skuCode)`
        * **Logic:**
            1.  Calls `inventoryRepository.findBySkuCodeIn(skuCode)`.
            2.  Maps the resulting `Inventory` objects to `InventoryResponse` DTOs.
            3.  The business logic is simple: `isInStock` is `true` if the found inventory record has a `quantity > 0`.

* **Repository: `InventoryRepository.java`**
    * **Type:** `interface extends JpaRepository<Inventory, Long>`
    * **Custom Method:** `List<Inventory> findBySkuCodeIn(List<String> skuCode);`
        * This is a custom query method. Spring Data JPA will automatically generate the implementation based on the method name.

* **Entity: `Inventory.java`**
    * **Annotations:** `@Entity`, `@Table(name = "t_inventory")`
    * **Fields:** `id`, `skuCode`, `quantity`.

### 2.2. Database Migration Script

* **File:** `src/main/resources/db/migration/V1__init.sql`
    ```sql
    CREATE TABLE t_inventory (
        id BIGINT NOT NULL AUTO_INCREMENT,
        sku_code VARCHAR(255),
        quantity INTEGER,
        PRIMARY KEY (id)
    );
    ```
