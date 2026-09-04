# Engineering Decision Record (EDR)

This document outlines the most important architectural and engineering decisions made during the development of this Inventory and Order Management platform.

### Why inventory is separated into SKU + warehouse inventory
A naive approach is to store a single `stock_quantity` on the `Product` table. However, physical inventory exists in specific physical locations. By separating inventory into `Inventory (ProductVariant + Warehouse)`, the system models reality. It allows the business to define warehouse allocation logic (priority routing), conduct warehouse-specific audits, and physically transfer stock between locations, which is impossible with a single global integer.

### Why current inventory and ledger are separate
Storing only the `quantityOnHand` is a lossy architecture. If the quantity drops from 10 to 8, the business has no idea *why* (Was it an order? A manual correction? Shrinkage?). 
By utilizing a **Ledger** (`InventoryTransaction`), we append every physical change to a history table. The current `quantityOnHand` on the `Inventory` table is simply a denormalized cache of the sum of all ledger entries, providing O(1) read performance while guaranteeing 100% auditability.

### Why reservations don't immediately reduce physical stock
E-commerce checkout flows are multi-stage. If we deduct physical stock immediately when an order is *created*, and the user abandons the checkout or payment fails, we have artificially constrained our physical supply (dead stock).
By separating `quantityOnHand` and `quantityReserved`:
- `quantityOnHand` represents true physical reality.
- `quantityReserved` represents financial promises.
This allows us to release expired reservations cleanly without faking a "refund" ledger transaction for stock that never physically left the warehouse.

### Why optimistic locking/database concurrency control is used
We use JPA `@Version` instead of pessimistic locking (`SELECT FOR UPDATE`). E-commerce workloads are typically read-heavy with occasional write contention on popular items. Pessimistic locks hold open database connections and can quickly cause connection pool exhaustion or deadlocks during flash sales. Optimistic locking pushes the conflict detection to the very end of the transaction (at `COMMIT` time). If a conflict occurs, the second thread immediately fails safely, preventing overselling without holding expensive database locks.

### Why idempotency is database-backed
Network requests fail. Clients retry. A retry on a checkout could result in a customer being charged twice and stock being deducted twice. 
We use a database-backed idempotency table with a unique constraint on `(user_id, operation_type, idempotency_key)`. When two identical requests hit the system concurrently, one inserts the record successfully, and the other hits a PostgreSQL `DataIntegrityViolationException`, safely aborting the transaction. This guarantees exactly one business effect without needing a distributed lock manager like Redis.

### Why transfers are explicit domain operations
Transfers (moving stock from Warehouse A to Warehouse B) could technically be done via two manual adjustments. But this creates a dangerous transaction boundary: if the deduction succeeds but the addition fails, stock vanishes into the void. 
By making `InventoryTransfer` an explicit entity, we wrap the source deduction and destination addition into a single ACID transaction. It either fully succeeds or fully rolls back, conserving total system stock.

### Why audit logs are separate from inventory transactions
The `InventoryTransaction` ledger tracks *physical* movement. The `AuditLog` tracks *administrative* action (who did what, and when). While an inventory transfer creates two ledger entries (one out, one in), it only creates *one* audit log (Admin X initiated a transfer). Mixing these concepts pollutes the financial ledger with operational metadata.

### Why external infrastructure such as Redis/Kafka was intentionally avoided
Simplicity and maintainability. Kafka is excellent for asynchronous event-driven architectures, and Redis is excellent for distributed caching and locking. However, they introduce immense operational overhead (deployment, monitoring, network partitions). 
For a standard modular monolith, PostgreSQL provides robust ACID guarantees, unique constraints, and check constraints that solve 99% of concurrency and consistency issues. We can achieve safe idempotency, safe reservations, and safe ledger appends using solely relational database features.

### What the system guarantees
- **No Physical Overselling**: `quantityOnHand` can never drop below 0 due to PostgreSQL check constraints and Java domain logic.
- **No Logical Overselling**: `quantityOnHand` can never drop below `quantityReserved`.
- **Idempotency**: The same `Idempotency-Key` will never result in two created orders.
- **Ledger Consistency**: The sum of ledger transactions equals the physical stock.

### What the system does NOT guarantee
- **Distributed Exactly-Once Processing**: Idempotency is scoped to our database. If an external payment gateway is added later, 2PC (Two-Phase Commit) or an outbox pattern would be needed.
- **Infinite Scalability**: This is a monolith utilizing a single primary PostgreSQL writer. It will handle thousands of concurrent checkouts via optimistic locking, but it is not globally sharded like a planetary-scale system.
