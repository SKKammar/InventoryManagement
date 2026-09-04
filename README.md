# Inventory & Order Management Platform (Technical Specification)

A highly concurrent, production-grade Spring Boot 3 API for inventory reservation, multi-warehouse allocation, and idempotent order processing.

## 1. System Overview & Domain Boundaries
This system manages the backend lifecycle of a distributed e-commerce inventory system.
- **Product Domain**: Manages abstract `Product` entities and tangible `ProductVariant` (SKUs).
- **Inventory Domain**: Tracks physical `Inventory` mappings (`ProductVariant` + `Warehouse`), resolving concurrent allocations and maintaining an immutable `InventoryTransaction` ledger.
- **Order Domain**: Handles customer checkouts via an Idempotency-protected API, translating `OrderItems` into physical `InventoryReservations`, and transitioning orders through a strict state machine (`CREATED` -> `CONFIRMED` -> `PROCESSING` -> `COMPLETED`/`CANCELLED`).

## 2. Technical Stack & Architecture
- **Framework**: Java 17, Spring Boot 3.2.0, Spring Security, Spring Data JPA (Hibernate).
- **Architecture**: Modular Monolith. Intentionally avoids distributed systems overhead (Kafka, Redis) by maximizing relational database guarantees.
- **Database**: PostgreSQL 15.
- **Migrations**: Flyway (State-based SQL versioning).
- **Testing**: JUnit 5, MockMvc, and Testcontainers (Dynamic containerized PostgreSQL injection).

## 3. Concurrency & Locking Strategy
The system handles severe flash-sale contention without connection-pool exhaustion:
- **Optimistic Locking**: `Inventory` and `Order` entities utilize `@Version` annotations. Concurrent writes (e.g., two threads modifying `quantityReserved`) result in an `ObjectOptimisticLockingFailureException` on `COMMIT` rather than deadlocking the database via `SELECT FOR UPDATE`.
- **Transaction Boundaries**: Standard `@Transactional` isolation is strictly enforced. Inventory deductions (`InventoryService.adjustStock`) and Ledger appends (`InventoryTransactionRepository.save`) are atomically bound to prevent phantom stock.

## 4. Idempotent Command Processing
Checkout requests are secured against double-clicks, network drops, and malicious replays:
- **Idempotency Key**: Clients must provide an `Idempotency-Key` UUID.
- **Hashing**: The `CreateOrderRequest` payload is canonically hashed (SHA-256).
- **Persistence & Locking**: Handled via `IdempotencyRecord` table with a unique composite index on `(user_id, operation_type, idempotency_key)`.
- **Conflict Resolution**: If a concurrent request arrives, a `DataIntegrityViolationException` is caught, and the API returns `409 CONFLICT: PROCESSING`. Re-used keys with altered payloads return `409 CONFLICT: HASH MISMATCH`. Success states immediately return the originally generated `OrderDTO`.

## 5. Inventory Correctness & The Ledger
- **Physical vs Reserved**: The system distinguishes between `quantityOnHand` (physical reality) and `quantityReserved` (financial promises). `availableQuantity = quantityOnHand - quantityReserved`.
- **Immutable Ledger**: The `inventory_transactions` table acts as the source of truth. The denormalized `quantityOnHand` is simply a cache of `SUM(ledger.quantity_change)`.
- **Transfers**: `InventoryTransfer` is an explicit domain entity, wrapping a source deduction and a destination addition within a single ACID transaction to prevent stock vanishing.
- **Database Constraints**: Defense-in-depth is enforced via PostgreSQL `CHECK (quantity_on_hand >= 0)` and `CHECK (quantity_reserved >= 0)` to guarantee invariants even if application logic fails.

## 6. Multi-Warehouse Allocation Routing
When an order is placed, `InventoryReservationService` queries `Inventory` sorted by `Warehouse.priority`. It loops through the inventory, algorithmically fragmenting the request across warehouses (e.g., reserving 6 units from `WH-MAIN` and 4 units from `WH-SECONDARY`), writing explicit `OrderItemAllocation` records mapped to the reservations.

## 7. State Machine Idempotency
Order state transitions (`cancelOrder`, `completeOrder`, `processOrder`) are strictly guarded. To support robust external integrations (webhooks, client retry queues), transitions are fully idempotent—attempting to `cancelOrder` on an already `CANCELLED` order safely returns a `200 OK` rather than throwing a `500 IllegalStateException`.

## 8. Security & API Protection
- **Stateless JWT**: Spring Security intercepts requests via a custom `JwtAuthenticationFilter`, parsing HttpOnly cookies or Bearer tokens.
- **RBAC**: Endpoints are strictly protected using `@PreAuthorize("hasRole('ADMIN')")` and custom data-ownership logic.
- **Mass Assignment Defense**: The system exclusively uses DTOs (`CreateOrderRequest`, `OrderDTO`) with MapStruct. Entities are never exposed to or hydrated directly from the presentation layer.

## 9. Observability & Auditing
- **MDC Logging**: All logs include `%X{requestId}` and `%X{userId}` injected via Mapped Diagnostic Context for tracing.
- **Audit Trails**: Separate from the financial Ledger, an `AuditLog` table tracks operational metadata (e.g., which `Admin` cancelled an order or manually adjusted stock).
- **Actuator**: `/actuator/health` and `/actuator/metrics` expose system vitals and custom Micrometer metrics (e.g., checkout success/failure rates, optimistic lock contention).

## 10. Test Infrastructure
- **Testcontainers**: The `@SpringBootTest` suite dynamically provisions a real PostgreSQL 15 Docker container via `IntegrationTestBase.java` to ensure constraints and optimistic locking behave accurately.
- **Load Testing**: Programmatic benchmarking inside `LoadTestScenario.java` uses `ExecutorService` and `CountDownLatch` to simulate 50+ concurrent checkouts, verifying connection pooling and deadlock resiliency.

---

## Running Locally

Ensure Docker is installed and JDK 17 is configured.

```bash
# Start PostgreSQL via Docker Compose
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

## Running the Test Suite (Testcontainers)
Ensure the Docker daemon is running locally before executing the suite.

```bash
./mvnw clean test
```
