# Transactions + Roost Core State Machines

**Week 13** · [DAILY-PLAN.md](../../DAILY-PLAN.md)

## 1. Definition & Why

Week 13 focused on transaction boundaries, propagation, isolation, and domain state transitions in Roost.

The main goal was to make monitor creation transactional across multiple tables and introduce the first real incident state machine.

Roost now demonstrates:

- Transactional monitor creation across `monitors`, `probe_schedule`, and `audit_logs`
- Incident lifecycle states: `OPEN → ACK → RESOLVED`
- `REQUIRED` propagation for normal service operations
- `REQUIRES_NEW` for independent audit logging
- PostgreSQL isolation concepts
- Dynamic filtering with Spring Data `Specification`
- State-transition validation
- Integration tests against real PostgreSQL

The core architecture is:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

Transactions belong primarily at the service/use-case boundary because a service operation may span multiple repositories.

---

## 2. Mental Model

### `@Transactional`

`@Transactional` is Spring's declarative transaction boundary.

```text
@Transactional service method
        ↓
Spring transaction management
        ↓
Database connection / transaction
        ↓
PostgreSQL
```

It is not itself the database transaction. Spring uses it to tell the transaction manager how the database work should be grouped.

A successful transaction:

```text
BEGIN
  operation A
  operation B
  operation C
COMMIT
```

A rollback:

```text
BEGIN
  operation A
  operation B
  operation C
  exception
ROLLBACK
```

The important boundary is the **service method**, not individual repository calls.

### `REQUIRED`

Default propagation.

```text
Service A
  @Transactional
       ↓
Repository
       ↓
DB
```

If a transaction already exists, `REQUIRED` joins it. Otherwise Spring starts one.

### `REQUIRES_NEW`

Suspends the current transaction and starts a completely independent transaction.

```text
Outer transaction
      │
      ├── operation
      │
      └── REQUIRES_NEW
              │
              ├── independent transaction
              └── COMMIT
      │
      └── outer transaction continues
```

Therefore an outer rollback does not automatically roll back the committed `REQUIRES_NEW` work.

### Important practical lesson

`REQUIRES_NEW` is **not automatically better** for auditing.

During monitor creation we initially had:

```text
MonitorService.createMonitor()
    REQUIRED
       ↓
    save Monitor
       ↓
AuditService.logMonitorEvent()
    REQUIRES_NEW
```

PostgreSQL rejected the audit insert because the newly created monitor was not committed and therefore was not visible to the independent transaction:

```text
audit_logs.monitor_id → monitors.id
                         ↑
                 not committed yet
```

This produced a foreign-key violation.

The correct design for `MONITOR_CREATED` was to let the audit participate in the same transaction:

```text
createMonitor()
    REQUIRED
       ├── Monitor
       ├── ProbeSchedule
       └── MONITOR_CREATED audit
              ↓
           COMMIT
```

`REQUIRES_NEW` remains appropriate for the incident audit demonstration where the referenced incident already exists and the audit intentionally needs independent transaction semantics.

---

## 3. Roost Code / Practice Summary

### Layered structure

Relevant packages:

```text
src/main/java/com/chandu/roost/
├── controller/
│   └── IncidentController.java
├── entity/
│   ├── AuditLog.java
│   ├── Incident.java
│   ├── IncidentStatus.java
│   └── ProbeSchedule.java
├── repository/
│   ├── AuditLogRepository.java
│   ├── IncidentRepository.java
│   └── ProbeScheduleRepository.java
├── service/
│   ├── AuditService.java
│   ├── IncidentService.java
│   └── MonitorService.java
└── specification/
    └── MonitorSpecification.java
```

### Incident state machine

`src/main/java/com/chandu/roost/entity/IncidentStatus.java`

```java
public enum IncidentStatus {
    OPEN,
    ACK,
    RESOLVED
}
```

The acknowledge operation enforces the valid transition:

```java
if (incident.getStatus() != IncidentStatus.OPEN) {
    throw new IllegalStateException(
            "Only OPEN incidents can be acknowledged"
    );
}

incident.setStatus(IncidentStatus.ACK);
incident.setAcknowledgedAt(Instant.now());
```

This prevents an invalid transition such as:

```text
ACK → ACK
RESOLVED → ACK
```

### Monitor creation transaction

`MonitorService.createMonitor(...)`

```java
@Transactional
public Monitor createMonitor(MonitorRequest request) {
    // load workspace
    // create + save monitor
    // create + save probe schedule
    // write MONITOR_CREATED audit
    // commit everything together
}
```

The operation spans multiple repositories but represents one business use case.

### Incident acknowledgement

`IncidentService.acknowledge(...)`

```java
@Transactional
public void acknowledge(Long incidentId) {
    Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(...);

    // OPEN → ACK
    incident.setStatus(IncidentStatus.ACK);
    incident.setAcknowledgedAt(Instant.now());

    incidentRepository.save(incident);

    auditService.logStateChange(
            incident,
            "INCIDENT_ACKNOWLEDGED"
    );
}
```

The state change is transactional while the audit service uses `REQUIRES_NEW`.

### Audit model

`AuditLog` now supports either:

```text
incident_id
```

or:

```text
monitor_id
```

This was introduced through:

`src/main/resources/db/migration/V8__generalize_audit_logs.sql`

The current schema permits either audit owner but does not yet enforce an exactly-one-owner constraint.

---

## 4. Interview Points

- `@Transactional` defines a Spring-managed transaction boundary.
- `REQUIRED` is the default propagation mode.
- `REQUIRES_NEW` suspends the current transaction and starts an independent one.
- `REQUIRES_NEW` work can survive an outer transaction rollback if it commits successfully.
- `RuntimeException` and `Error` trigger rollback by default; checked exceptions do not unless configured with `rollbackFor`.
- Self-invocation can bypass Spring's transactional proxy, so propagation behavior may not work when a method calls another `@Transactional` method on the same bean.
- Transaction boundaries usually belong in the service layer because business operations may span multiple repositories.
- `readOnly = true` is a transaction hint and may allow optimizations; it should not be treated as a security mechanism preventing writes.
- PostgreSQL's default isolation level is `READ COMMITTED`.
- `READ COMMITTED` allows later statements in the same transaction to see data committed after an earlier statement.
- `REPEATABLE READ` provides a consistent view for repeated reads within a transaction.
- A phantom read occurs when the result set of a query changes because another transaction inserts or deletes matching rows.
- `Specification` is useful when optional filters need to be dynamically combined.
- `Pageable` represents requested pagination/sorting; `PageRequest` is one implementation.
- `Page` normally requires content plus a count query; `Slice` can determine whether another page exists without the total-count query.
- State machines should enforce valid transitions rather than allowing arbitrary status mutation.
- `REQUIRES_NEW` can cause foreign-key visibility problems when the new transaction references data created but not yet committed by the suspended outer transaction.

---

## 5. Common Mistakes

- Treating `@Transactional` as if it were the database transaction itself.
- Assuming every repository call automatically creates an independent transaction.
- Putting a multi-repository business transaction in the controller.
- Using `REQUIRES_NEW` merely because "audit should be independent."
- Forgetting that an independent transaction cannot see uncommitted data from the suspended transaction.
- Calling a `@Transactional` method from another method in the same bean and expecting Spring propagation to apply.
- Assuming `readOnly = true` makes writes impossible.
- Allowing invalid state transitions such as `ACK → ACK`.
- Confusing transaction rollback with Java object rollback.
- Assuming an index will always be used; PostgreSQL's optimizer can correctly choose a sequential scan for small tables.
- Treating `Specification` as a replacement for pagination; they solve different problems.
- Using a generic `RuntimeException` for every production error instead of domain-specific exceptions.

---

## 6. Quick Revision + Q&A

**Revision (30 sec):**

Spring's `@Transactional` defines a service-level transaction boundary. `REQUIRED` joins or creates a transaction; `REQUIRES_NEW` suspends the existing transaction and starts another one. This makes `REQUIRES_NEW` useful when an operation must commit independently, but it also means the new transaction cannot see uncommitted data from the suspended transaction. Roost uses a normal transaction for monitor creation so monitor, schedule, and creation audit commit atomically, while incident state-change auditing demonstrates independent `REQUIRES_NEW` behavior. Incident state transitions are explicitly controlled: `OPEN → ACK → RESOLVED`.

### Q&A

**Q1. What does `@Transactional` do?**

It tells Spring to execute the method within a transaction managed by Spring's transaction infrastructure.

**Q2. Is `@Transactional` itself the database transaction?**

No. It defines the Spring transaction boundary and behavior. The transaction manager ultimately coordinates the database transaction.

**Q3. What is the default propagation mode?**

`REQUIRED`.

**Q4. What does `REQUIRED` mean?**

Join the existing transaction if one exists; otherwise create a new transaction.

**Q5. What does `REQUIRES_NEW` mean?**

Suspend the current transaction and execute the method in a new independent transaction.

**Q6. Why can `REQUIRES_NEW` be dangerous during entity creation?**

The new transaction cannot see uncommitted rows created by the suspended outer transaction. A foreign-key reference to such a row can therefore fail.

**Q7. Why does Roost use a normal transaction for `MONITOR_CREATED` audit?**

Because monitor creation, schedule creation, and its audit represent one atomic operation and the audit references the newly created monitor.

**Q8. Why keep `REQUIRES_NEW` for incident state-change auditing?**

It demonstrates an audit operation that can commit independently from the surrounding business transaction.

**Q9. What happens if the outer transaction rolls back after a successful `REQUIRES_NEW` audit?**

The independently committed audit remains committed.

**Q10. Which exceptions cause Spring transactions to roll back by default?**

Unchecked `RuntimeException` and `Error`. Checked exceptions require explicit rollback configuration such as `rollbackFor`.

**Q11. What is the self-invocation problem?**

A method calling another transactional method on the same object can bypass Spring's proxy, preventing the expected transactional interception and propagation behavior.

**Q12. Where should transaction boundaries generally be placed?**

At the service/use-case layer where a business operation may span multiple repositories.

**Q13. What is the PostgreSQL default isolation level?**

`READ COMMITTED`.

**Q14. What is a non-repeatable read?**

Reading the same row twice within a transaction and seeing different committed values because another transaction updated it between the reads.

**Q15. What is a phantom read?**

Repeating a query and receiving a different set of matching rows because another transaction inserted or deleted rows.

**Q16. What is a state machine in the Roost incident model?**

A controlled set of states and valid transitions. For example:

```text
OPEN → ACK → RESOLVED
```

An invalid transition such as `ACK → ACK` is rejected.

**Q17. Why is `Specification` useful in the monitor API?**

It allows optional filters such as workspace and monitor type to be composed dynamically without creating a separate repository method for every filter combination.

**Q18. What is the difference between `Pageable` and `PageRequest`?**

`Pageable` is the abstraction representing pagination and sorting information; `PageRequest` is a concrete implementation used to create it.

**Q19. What is the difference between `Page` and `Slice`?**

`Page` provides total-element/page information and normally requires a count query. `Slice` focuses on whether another slice exists and can avoid the total-count query.

**Q20. Why should controllers not directly call repositories for business operations?**

That bypasses service-level business rules and transaction boundaries and makes the application harder to maintain and test.

**Q21. What practical transaction bug did Roost expose this week?**

A `REQUIRES_NEW` monitor audit attempted to reference a monitor that existed only in the uncommitted outer transaction, causing a PostgreSQL foreign-key violation.

**Q22. How was that bug fixed?**

The monitor-creation audit was changed to participate in the outer transaction instead of using `REQUIRES_NEW`.

**Q23. How did we verify the transaction behavior?**

With Spring Boot integration tests against PostgreSQL and direct database verification using Docker's `psql`.

**Q24. What does the current Roost monitor creation use as its transaction boundary?**

`MonitorService.createMonitor()` is the transaction boundary covering monitor, probe schedule, and monitor-creation audit.

**Q25. What does the current incident acknowledgement operation do?**

It verifies the incident is `OPEN`, changes it to `ACK`, sets `acknowledgedAt`, saves it, and records `INCIDENT_ACKNOWLEDGED` through the audit service.

**Q26. What has been demonstrated but is not yet a complete automated monitoring engine?**

The persistence and transaction foundations are in place, but the full production-style scheduling/probing loop that periodically executes HTTP probes and automatically opens/resolves incidents remains future work.

### Key Takeaway

The important Week 13 lesson is not simply how to add `@Transactional`. It is how to choose the **correct transaction boundary and propagation strategy for a business operation**.

For Roost:

```text
Monitor creation
    → one atomic transaction

Incident state change
    → business transaction
       +
       independent audit transaction
```

And the PostgreSQL foreign-key failure during development demonstrated why transaction visibility matters when choosing `REQUIRES_NEW`.
