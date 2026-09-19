# Week 12 — Data Layer Interview Q&A

## JPA Fundamentals

### 1. What is JPA?

JPA (Jakarta Persistence API) is a specification for mapping Java objects to relational database tables and managing their persistence.

It defines concepts such as entities, relationships, persistence context, and JPQL. Hibernate is a commonly used implementation of JPA.

---

### 2. What is the difference between JPA and Hibernate?

* **JPA** is the specification/API.
* **Hibernate** is an implementation of that specification.

Spring Data JPA sits above JPA and provides repository abstractions that reduce boilerplate data-access code.

---

### 3. Why do we use `@Entity`?

`@Entity` tells JPA that a Java class represents a persistent entity whose instances can be stored in a relational database.

For example:

```java
@Entity
@Table(name = "monitors")
public class Monitor {
    ...
}
```

JPA then maps the entity's fields to columns in the `monitors` table.

---

### 4. Why do JPA entities need a no-argument constructor?

JPA needs to instantiate entities itself when loading database rows.

A protected no-argument constructor is commonly used:

```java
protected Monitor() {
}
```

The constructor doesn't need to be public.

---

### 5. Why are records generally unsuitable as JPA entities?

Records are immutable and final, while JPA commonly requires entities to be instantiated and managed through mechanisms such as a no-argument constructor, mutable state, and proxying/lazy loading.

Records are therefore better suited to DTOs such as `MonitorRequest` and `MonitorResponse`.

---

### 6. Why did we configure `ddl-auto: validate` instead of `update`?

Because Flyway owns our database schema.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

`validate` checks that the entity mappings match the existing schema without modifying it.

Using `update` would allow Hibernate to modify the schema independently of Flyway, creating two competing schema-management mechanisms.

---

## Spring Data JPA

### 7. What does `JpaRepository` provide?

`JpaRepository<Entity, ID>` provides common persistence operations such as:

* `save`
* `findById`
* `findAll`
* `delete`
* `count`
* pagination
* sorting
* flushing and batch-related operations

For example:

```java
public interface MonitorRepository
        extends JpaRepository<Monitor, Long> {
}
```

This eliminates much of the repetitive repository implementation code.

---

### 8. How does Spring Data JPA implement a method like `findByWorkspaceIdAndNameContainingIgnoreCase(...)`?

Spring Data parses the method name and builds the query from its structure.

For example:

```text
findByWorkspaceId
And
NameContaining
IgnoreCase
```

is interpreted using the entity's properties and Spring Data query keywords.

If the property or method structure is invalid, the application can fail during startup rather than silently generating an incorrect query.

---

### 9. When would you use `@Query` instead of a derived query method?

Use `@Query` when the query becomes too complex or awkward to express through a method name.

For example, our EntityGraph demonstration used:

```java
@Query("select w from Workspace w")
@EntityGraph(attributePaths = {"monitors"})
List<Workspace> findAllWithMonitorsUsingGraph();
```

`@Query` defines **what to query**, while `@EntityGraph` defines **which relationships should be fetched with that query**.

---

### 10. What is the difference between JPQL and a native query?

**JPQL** operates using the JPA entity model:

```text
select m from Monitor m
```

rather than directly referring to database tables.

A native query uses database-specific SQL.

Prefer JPQL when the query can naturally be expressed through the entity model. Native SQL is useful when you need database-specific functionality or SQL that is difficult to express through JPQL.

---

## Relationships

### 11. Which side owns a bidirectional `@ManyToOne` / `@OneToMany` relationship?

The `@ManyToOne` side is normally the owning side because it contains the foreign-key column.

In Roost:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "workspace_id")
private Workspace workspace;
```

The inverse side is:

```java
@OneToMany(mappedBy = "workspace")
private List<Monitor> monitors;
```

`mappedBy` tells JPA that `Workspace.monitors` is the inverse side.

---

### 12. Why should both sides of a bidirectional relationship be synchronized?

JPA relationship state exists in the in-memory object graph as well as in the database mapping.

If you only update one side, the Java objects can disagree about their relationship.

That's why we used a helper such as:

```java
public void addProbeResult(ProbeResult probeResult) {
    probeResults.add(probeResult);
    probeResult.setMonitor(this);
}
```

---

### 13. What are the default fetch types for `@ManyToOne` and `@OneToMany`?

By default:

* `@ManyToOne` → `EAGER`
* `@OneToMany` → `LAZY`

For performance-sensitive applications, explicitly choosing the fetch strategy is often preferable.

In Roost we explicitly made `Monitor.workspace` lazy:

```java
@ManyToOne(fetch = FetchType.LAZY)
```

---

### 14. What is `LazyInitializationException`?

It occurs when Hibernate tries to initialize a lazy relationship after the persistence context/session needed to load it has already been closed.

Typical solutions include:

* fetching the relationship inside the transaction
* using an EntityGraph
* using a suitable `JOIN FETCH`
* using DTO projections
* restructuring the query/use case

The correct solution depends on what data the endpoint actually needs.

---

### 15. What does `orphanRemoval = true` do?

It tells JPA to remove a child entity from the database when it is removed from its parent's managed collection.

It is useful when the child has no meaningful independent lifecycle outside the parent.

It should not be enabled automatically for every relationship.

---

## N+1 and Fetching

### 16. What is the N+1 query problem?

N+1 occurs when an application executes:

```text
1 query → load N parent records
N queries → load related data for each parent
```

So a request results in approximately:

```text
1 + N queries
```

instead of fetching the required data efficiently.

---

### 17. Why is N+1 particularly dangerous for list endpoints?

Suppose an endpoint loads 100 workspaces:

```text
1 query → 100 workspaces
100 queries → monitors for each workspace
```

That's 101 queries for one request.

The number of database round trips grows with the number of returned entities.

---

### 18. What is `@EntityGraph`?

`@EntityGraph` is a fetch-plan mechanism that tells JPA which relationships should be fetched for a particular repository operation.

For example:

```java
@EntityGraph(attributePaths = {"monitors"})
```

means the query should fetch the `monitors` relationship along with the requested workspaces.

It is **not an "N+1 annotation"**. It is one technique for controlling fetching and avoiding unnecessary lazy-loading queries.

---

### 19. What is `JOIN FETCH`?

`JOIN FETCH` is a JPQL technique for fetching an associated entity or collection as part of the query.

Conceptually:

```text
select w
from Workspace w
join fetch w.monitors
```

can load workspaces and their monitors together.

However, collection fetching combined with pagination can be problematic because the SQL result contains repeated parent rows. Hibernate may resort to in-memory pagination, which can be dangerous for large datasets.

---

### 20. How did we avoid that problem for `GET /monitors/{id}`?

We deliberately used separate queries:

```text
Query 1 → monitor
Query 2 → paginated probe results
Query 3 → COUNT(probe results)
```

Instead of trying to fetch the entire `probeResults` collection and paginate it afterward.

This keeps pagination at the database level.

---

## Pagination

### 21. How does Spring Data JPA implement pagination?

A repository method can accept a `Pageable`:

```java
Page<ProbeResult> findByMonitorId(
        Long monitorId,
        Pageable pageable);
```

Spring Data translates the pagination information into database-level pagination.

For example:

```text
page = 0
size = 2
```

causes the database query to return only the required page.

---

### 22. What is the difference between `Page` and `List`?

A `List` contains only the returned records.

A `Page` also provides pagination metadata such as:

* current page
* page size
* number of elements
* total elements
* total pages
* first/last-page information

The `Page` abstraction is useful when an API needs to expose pagination metadata to clients.

---

### 23. Why does a `Page` often execute a `COUNT` query?

Because Spring needs to determine metadata such as:

```text
totalElements
totalPages
```

For our request:

```text
GET /monitors/3?page=0&size=2
```

Hibernate executed:

```sql
select count(pr1_0.id)
from probe_results pr1_0
where pr1_0.monitor_id=?
```

in addition to the query retrieving the current page.

---

## Indexes and SQL

### 24. What is a database index?

An index is a data structure that helps the database locate rows more efficiently for supported access patterns.

It can significantly improve certain reads, but indexes aren't free.

They consume storage and add work to inserts, updates, and deletes.

---

### 25. What is the leftmost-prefix rule for a composite index?

For an index:

```sql
(monitor_id, probed_at DESC)
```

the first column is the leading column.

Queries that constrain `monitor_id` can effectively use the index and then benefit from the `probed_at` ordering.

That's why this access pattern matches our index:

```sql
WHERE monitor_id = ?
ORDER BY probed_at DESC
```

But an index beginning with `monitor_id` is not generally equivalent to an index beginning with `probed_at`.

---

### 26. Why shouldn't we index every column?

Indexes improve particular read patterns but introduce costs:

* additional disk/storage usage
* additional work during writes
* additional maintenance
* more indexes for the query planner to consider

Indexes should be driven by actual query patterns and workload characteristics.

---

### 27. Why didn't PostgreSQL necessarily use our new index?

Because an index existing doesn't mean the optimizer must use it.

PostgreSQL estimates the cost of different execution plans.

With a very small table, a sequential scan can legitimately be cheaper than using an index.

Therefore:

> **Adding an index does not guarantee an Index Scan.**

Use `EXPLAIN` / `EXPLAIN ANALYZE` and realistic data sizes to evaluate whether an index is useful.

---

### 28. How do you investigate a slow query?

Start with:

```sql
EXPLAIN ANALYZE
SELECT ...;
```

Look at:

* sequential vs index scans
* estimated vs actual rows
* execution time
* joins
* sorts
* filtering
* rows removed by filters
* expensive operations

Then determine whether the query, schema, indexes, or data distribution needs attention.

---

## Roost-Specific Questions

### 29. Explain the data-access flow for `POST /monitors`.

```text
MonitorController
      ↓
MonitorService
      ↓
WorkspaceRepository.findById()
      ↓
Create Monitor entity
      ↓
MonitorRepository.save()
      ↓
PostgreSQL
```

The workspace is looked up first so the new monitor can reference an existing `Workspace`.

---

### 30. Explain the data-access flow for `GET /monitors/{id}`.

```text
MonitorController
      ↓
MonitorService
      ├── MonitorRepository
      │      ↓
      │   Monitor
      │
      └── ProbeResultRepository
             ↓
          Page<ProbeResult>
                ↓
       Page<ProbeResultResponse>
                ↓
       MonitorDetailResponse
```

The probe history is independently paginated rather than loading the entire collection.

---

### 31. How would you detect N+1 in a real application?

Possible approaches include:

* Hibernate SQL logging during development
* integration tests with query-count assertions
* tools such as Hypersistence Utils' SQL statement counter
* application observability/metrics
* database monitoring

The important point is to detect excessive query counts rather than relying only on code inspection.

---

### 32. What did the SQL for our final monitor-detail endpoint look like?

For:

```text
GET /monitors/3?page=0&size=2
```

we observed:

```text
1. SELECT monitor
2. SELECT probe_results
   WHERE monitor_id = ?
   ORDER BY probed_at DESC
   FETCH FIRST ? ROWS ONLY
3. SELECT COUNT(probe_results)
```

There was no loop producing one query per probe result, so the endpoint did not exhibit an N+1 query pattern.

---

## Key Interview Takeaways

Be able to explain these without hesitation:

1. **JPA is a specification; Hibernate is an implementation.**
2. **Flyway owns the schema; Hibernate uses `validate`.**
3. **Spring Data repositories remove CRUD boilerplate.**
4. **Derived query names are parsed into queries.**
5. **`@ManyToOne` owns the foreign key; `mappedBy` marks the inverse side.**
6. **Lazy loading must be deliberate.**
7. **N+1 means one parent query followed by N related queries.**
8. **`@EntityGraph` controls the fetch plan; it doesn't magically eliminate every N+1.**
9. **Don't combine collection fetch joins blindly with pagination.**
10. **`Page<T>` gives database-backed pagination plus metadata.**
11. **Composite indexes depend on column order and access patterns.**
12. **The query optimizer decides whether an index is actually worthwhile.**
13. **Use `EXPLAIN ANALYZE` to inspect real query behavior.**
14. **For paginated child history, separate parent and child queries are often cleaner than fetching the entire collection.**
15. **Indexes improve selected reads but impose storage and write-maintenance costs.**
