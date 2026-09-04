# Interview Defense Guide

This document maps the engineering challenges solved in this project to standard Software Development Engineer (SDE) interview topics. 

## 1. Inventory Concurrency
**Problem**: During a flash sale, 100 users try to buy the last 5 items simultaneously.
**Naive solution**: Read `available_stock = 5`, check if `> 0`, then update `available_stock = 4`. Due to race conditions (read-modify-write), all 100 users read `5` and all successfully checkout, resulting in -95 stock.
**Production solution**: We use Optimistic Locking via JPA `@Version`. When 100 threads read the `Inventory` row, they all see version 1. The first thread to commit updates the row to version 2. The other 99 threads attempt to write `WHERE id = X AND version = 1`. The database reports 0 rows updated, Spring throws an `ObjectOptimisticLockingFailureException`, and the transactions safely roll back.
**Tradeoff**: High contention on a single SKU causes many aborted transactions instead of queuing them. This is preferable for system stability over pessimistic locking which would exhaust the connection pool.

## 2. Idempotency
**Problem**: A customer clicks "Checkout" on a mobile app. The network drops before the response arrives. The app retries the request. The customer gets charged twice and two orders are created.
**Naive solution**: Trust the frontend to disable the submit button, or check if an order with the same items exists in the database.
**Production solution**: We require clients to send an `Idempotency-Key` (UUID). The backend attempts to insert this key into an `idempotency_records` table with a unique constraint on `(user_id, operation, key)`. 
**Failure scenario**: Two requests arrive exactly simultaneously.
**Testing strategy**: `IdempotentCheckoutTests` spins up an `ExecutorService` and fires two identical requests via a `CountDownLatch`. We assert that the database correctly throws a `DataIntegrityViolationException` on the second thread, resulting in exactly one successful order.

## 3. Order State Machine & Double Cancellations
**Problem**: A user attempts to cancel an order twice (due to lag).
**Naive solution**: Check if `status == CANCELLED` and throw a 500 `IllegalStateException`.
**Production solution**: A robust API should be idempotent on state changes. If `cancelOrder()` is called on an already `CANCELLED` order, the system intercepts the check and safely returns the existing `OrderDTO` with a 200 OK. 
**Tradeoff**: Returning success hides the fact that the client sent a redundant request, but it makes distributed integrations (like webhooks or retry queues) vastly more resilient since they don't crash on successful eventual consistency.

## 4. Multiwarehouse Allocation
**Problem**: An order for 10 items comes in, but Warehouse A has 6 and Warehouse B has 4.
**Naive solution**: Fail the order, or pick a random warehouse.
**Production solution**: We implemented a priority-based routing algorithm. Warehouses have a `priority` integer. The `InventoryReservationService` queries all active warehouses for the SKU, sorted by priority. It loops through the inventory, peeling off available stock (6 from A, then 4 from B) and creates multiple `OrderItemAllocation` records mapped back to the single `OrderItem`.
**Failure scenario**: Total available across all warehouses is 9. The loop calculates total available upfront; if `total < requested`, it immediately aborts before making any partial reservations.

## 5. Ledger vs Current State
**Problem**: A warehouse manager says "Why did our stock drop to 0 yesterday?"
**Naive solution**: A single `stock` integer column. No one knows why it changed.
**Production solution**: Event-sourcing inspired Ledger. Every physical change (Checkout, Transfer, Manual Add) creates an immutable `InventoryTransaction`. The `quantityOnHand` on the `Inventory` table is just a materialized cache of the ledger.
**Testing strategy**: `LedgerReconciliationTest` executes multiple random operations (additions, deductions, transfers) and asserts that `SUM(ledger.quantity_change) == inventory.quantity_on_hand`.

## 6. Security (Mass Assignment)
**Problem**: A malicious user sends `{"items": [{"id":1}], "status": "COMPLETED", "user": {"role": "ADMIN"}}` to the checkout endpoint.
**Naive solution**: The controller accepts the `Order` entity directly via `@RequestBody` and saves it. The attacker elevates their privileges.
**Production solution**: Complete isolation using DTOs. The `CreateOrderRequest` record only exposes `idempotencyKey` and `items`. The backend extracts the user explicitly from the JWT `SecurityContextHolder`, entirely ignoring any user data provided in the payload.

## 7. Database Constraints (Defense in Depth)
**Problem**: A bug in the Java code forgets to check if inventory drops below zero.
**Naive solution**: Rely solely on application-layer `if (stock < 0)` checks.
**Production solution**: Added a PostgreSQL `CHECK (quantity_on_hand >= 0)` constraint via Flyway. 
**Tradeoff**: If the Java layer fails, the database throws a hard SQL error, resulting in a 500 response. This is vastly preferable to silent data corruption (negative stock). Verified via `DatabaseConstraintTest`.
