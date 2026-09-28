# Week 13 — Transactions & State Management Interview Q&A

## Transactions

### 1. What does `@Transactional` do in Spring?

`@Transactional` defines a transaction boundary around a method. Spring manages the underlying database transaction and commits or rolls it back according to the method's outcome.

The annotation itself is not the database transaction; it tells Spring how the transaction should be managed.

---

### 2. Where should transaction boundaries usually be placed?

Usually in the **service layer**.

The service represents a business operation and can coordinate multiple repository operations that must succeed or fail together.

```text
Controller
    ↓
Service (@Transactional)
    ↓
Repository
    ↓
Database
```

---

### 3. What happens when a `@Transactional` method throws a `RuntimeException`?

By default, Spring rolls back the transaction for `RuntimeException` and `Error`.

In Roost, `IncidentService.resolve()` deliberately throws a `RuntimeException` after creating the audit record to demonstrate rollback behavior.

---

### 4. What is `Propagation.REQUIRED`?

`REQUIRED` is the default propagation mode.

* Existing transaction → join it.
* No existing transaction → create one.

In Roost, `logMonitorEvent()` uses normal `REQUIRED` behavior so its audit participates in the monitor-creation transaction.

---

### 5. What is `Propagation.REQUIRES_NEW`?

`REQUIRES_NEW` suspends the current transaction and starts an independent transaction.

The inner transaction can commit even if the outer transaction later rolls back.

Roost uses this for incident state-change audit logging.

---

### 6. Why is `AuditService` a separate Spring bean?

Spring's `@Transactional` behavior is normally applied through a proxy.

If a transactional method is called through `this.method()`, the call can bypass the proxy.

Therefore:

```text
IncidentService → AuditService
```

allows the `REQUIRES_NEW` transaction interceptor to be applied correctly.

---

### 7. What is the self-invocation problem?

Consider:

```java
public void methodA() {
    methodB();
}

@Transactional
public void methodB() {
}
```

The internal call may not pass through Spring's transactional proxy, so the transactional behavior on `methodB()` may not be applied as expected.

A separate Spring bean is a common solution.

---

## Isolation & Rollback

### 8. What is transaction isolation?

Isolation controls what one transaction can observe from concurrent transactions.

Important levels:

```text
READ_COMMITTED
REPEATABLE_READ
SERIALIZABLE
```

Stronger isolation provides stronger consistency guarantees but can reduce concurrency or increase contention.

---

### 9. Explain dirty read, non-repeatable read, and phantom read.

**Dirty read:** reading uncommitted data from another transaction.

**Non-repeatable read:** reading the same existing row twice and getting different committed values.

**Phantom read:** repeating a query and getting a different set of matching rows because another transaction changed the matching rows.

---

### 10. What is PostgreSQL's default isolation level?

PostgreSQL defaults to:

```text
READ COMMITTED
```

Roost's Hibernate startup output also showed `READ_COMMITTED`.

---

### 11. Why does `REQUIRES_NEW` matter in the Roost audit use case?

The requirement is:

> The audit record must survive even if the main operation rolls back.

Therefore:

```text
resolve()
    ↓
outer transaction
    ↓
logStateChange()
    ↓
REQUIRES_NEW
    ↓
audit COMMIT
    ↓
resolve() fails
    ↓
outer ROLLBACK
```

The audit remains committed.

---

## Rich Domain & State Machine

### 12. What is a rich domain model?

A rich domain model puts important business rules and state transitions inside domain entities.

In Roost, `Incident` owns the state-transition rules rather than allowing services to freely modify its status.

---

### 13. What state transitions does the Roost `Incident` support?

```text
OPEN → ACK → RESOLVED
```

Invalid transitions are rejected by the entity.

For example:

```text
OPEN → RESOLVED     ❌
ACK → ACK           ❌
RESOLVED → ACK      ❌
RESOLVED → RESOLVED ❌
```

---

### 14. Why should `Incident` guard its own state transitions?

Because the entity should protect its own business invariants regardless of which caller attempts the operation.

For example:

```java
public void resolve() {
    if (status != IncidentStatus.ACK) {
        throw new IllegalStateException(
            "Only ACK incidents can be resolved"
        );
    }

    this.status = IncidentStatus.RESOLVED;
    this.resolvedAt = Instant.now();
}
```

The service coordinates the use case; the entity protects the state-machine invariant.

---

### 15. What is the difference between entity responsibility and service responsibility?

**Entity:**

* Domain state
* Business invariants
* State transitions

**Service:**

* Transaction boundary
* Loading entities
* Repository coordination
* Cross-entity/business-operation orchestration
* Calling other services

For Roost:

```text
Incident
    → owns OPEN → ACK → RESOLVED

IncidentService
    → coordinates the transactional operation
```

---

## Specifications & Pagination

### 16. What are Spring Data JPA Specifications?

Specifications allow dynamic query predicates to be composed programmatically.

Roost uses them for optional monitor filters such as:

```text
workspaceId
type
```

The repository implements:

```java
JpaSpecificationExecutor<Monitor>
```

allowing specifications to be combined with pagination.

---

### 17. Why didn't we add `status` to `Monitor` for the filtering example?

Because the actual Roost domain model does not contain `Monitor.status`.

Probe outcomes are represented through `ProbeResult`.

A generic roadmap example should not cause us to introduce a domain field that the actual application does not need.

---

### 18. What is the difference between `Page` and `Slice`?

`Page<T>` provides total-count information such as:

```text
totalElements
totalPages
```

and normally requires a count query.

`Slice<T>` primarily tells you whether another slice exists and can avoid the total-count query.

Use `Page` when total counts are required; use `Slice` when they are not.

---

## Roost Architecture & Practical Design

### 19. Why must monitor creation and `ProbeSchedule` creation be in the same transaction?

Creating a monitor without its schedule would leave the system in an incomplete state.

Therefore:

```text
MonitorService.createMonitor()
    @Transactional
        ├── save Monitor
        ├── save ProbeSchedule
        └── save monitor audit
```

All three operations should succeed or roll back together.

---

### 20. Why did `REQUIRES_NEW` cause a foreign-key problem during monitor creation?

The monitor was created inside an uncommitted outer transaction.

A `REQUIRES_NEW` transaction is independent and cannot see the uncommitted monitor row from the suspended outer transaction.

Therefore an audit insert referencing that monitor could fail the foreign-key constraint.

We changed monitor-event auditing to normal `REQUIRED` behavior so it joins the outer transaction.

---

### 21. How does the Roost transaction test prove that `REQUIRES_NEW` works?

The test performs:

```text
OPEN
  ↓
ACK
  ↓
resolve()
  ├── Incident → RESOLVED
  ├── REQUIRES_NEW audit → COMMIT
  └── forced RuntimeException
          ↓
      outer ROLLBACK
```

Afterwards:

```text
Incident status = ACK
resolved_at     = null

Audit:
INCIDENT_RESOLVED = still present
```

This proves the audit transaction committed independently of the failed outer transaction.

---

### 22. What is the main transaction-design lesson from Roost?

A transaction boundary should match the **business operation**.

If operations must succeed or fail together, they should participate in the same transaction.

If an operation must survive the rollback of its caller, it may require an independent transaction such as `REQUIRES_NEW`.

For Roost:

```text
Monitor creation
    → one transaction

Incident resolution
    → outer transaction

Incident audit
    → independent transaction
```

This distinction is the core transaction-design lesson from Week 13.
