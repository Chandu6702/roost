# N+1 Problem & Fetch Strategies

**Week 12 · Day 4** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

The **N+1 problem** occurs when loading a list of parent entities requires one SQL query, and then accessing a lazy child relationship for each parent triggers an additional query per parent. This results in **1 + N SQL queries** instead of fetching the required data efficiently. In Roost, loading multiple `Workspace` entities and then accessing their `monitors` collection demonstrated this problem. We learned two important ways to control fetching: JPQL `JOIN FETCH` and Spring Data JPA's `@EntityGraph`.

## 2. Mental Model

Without a fetch strategy:

```text
Load Workspaces
      ↓
   1 SQL query
      ↓
Workspace 1 → load monitors → SQL
Workspace 2 → load monitors → SQL
Workspace 3 → load monitors → SQL
      ↓
1 + N queries
```

With a fetch strategy:

```text
Load Workspaces + required Monitors
              ↓
       fetch relationship
              ↓
       fewer SQL queries
```

### `JOIN FETCH`

```text
@Query
   ↓
Defines the JPQL query
   ↓
JOIN FETCH explicitly fetches the relationship
```

### `@EntityGraph`

```text
Repository query method
        +
@EntityGraph
        ↓
Controls which relationships are fetched
```

Important:

**`@EntityGraph` does not itself define the repository query.**

For our custom method, both are used:

```java
@EntityGraph(attributePaths = "monitors")
@Query("select w from Workspace w")
List<Workspace> findAllWithMonitorsUsingGraph();
```

`@Query` defines **what to query**; `@EntityGraph` defines **what relationship to fetch with it**.

A custom repository method without a recognized query definition caused:

```text
No property 'findAllWithMonitorsUsingGraph' found for type 'Workspace'
```

Spring Data tried to interpret the method name as a derived query. This taught us that `@EntityGraph` is not a replacement for query definition.

## 3. My Roost Code

### N+1 reproduction

`src/main/java/com/chandu/roost/workspace/WorkspaceService.java`

The service loaded all workspaces and accessed the lazy monitor collection:

```java
List<Workspace> workspaces =
        workspaceRepository.findAll();

for (Workspace workspace : workspaces) {
    workspace.getMonitors().size();
}
```

With 3 workspaces, Hibernate produced:

```text
1 query → workspaces
3 queries → monitors for each workspace

Total = 1 + 3 = 4 queries
```

### Fix 1 — `JOIN FETCH`

`WorkspaceRepository.java`

```java
@Query("select w from Workspace w join fetch w.monitors")
List<Workspace> findAllWithMonitors();
```

Hibernate generated a single SQL query containing a join between `workspaces` and `monitors`.

### Fix 2 — `@EntityGraph`

For the custom repository method:

```java
@EntityGraph(attributePaths = "monitors")
@Query("select w from Workspace w")
List<Workspace> findAllWithMonitorsUsingGraph();
```

Hibernate generated one query with a join and fetched the `monitors` relationship.

### SQL logging used for verification

`application.yml`:

```yaml
spring:
  jpa:
    show-sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
```

The SQL logs were used to verify the number of queries rather than assuming that the optimization worked.

## 4. Interview Points

- **What is N+1?** One query loads the parent list and N additional queries load the lazy child relationship for each parent.
- **Why does it happen?** A lazy association is accessed separately for each loaded parent.
- **How can N+1 be fixed?** Depending on the use case: `JOIN FETCH`, `@EntityGraph`, DTO/projection queries, batch fetching, or other query/fetch strategies.
- **What is `@EntityGraph`?** A way to specify which entity relationships should be fetched for a particular repository operation.
- **Does `@EntityGraph` require `@Query`?** No. It can be applied to inherited repository methods or valid derived-query methods. For our custom `findAllWithMonitorsUsingGraph()` method, `@Query` was needed because that method name did not define a valid derived query.
- **`JOIN FETCH` vs `@EntityGraph`?** Both can solve N+1. `JOIN FETCH` expresses the fetch directly in JPQL; `@EntityGraph` expresses the fetch plan separately from the query and fits naturally with Spring Data repository methods.
- **Why be careful with collection `JOIN FETCH` and pagination?** Hibernate may be unable to apply SQL-level pagination and may perform pagination in memory, which can become expensive for large datasets.
- **When does N+1 hurt most?** Usually list endpoints where N can grow with the number of parent records.
- **How can N+1 be detected?** SQL logging, query-count assertions in integration tests, request tracing, and database/observability metrics.
- **What does `Hibernate.initialize()` do?** It forces a lazy proxy or collection to initialize while the persistence context is still open.
- **Does `Hibernate.initialize()` automatically solve N+1?** No. Calling it separately for each parent can still produce 1 + N queries.

## 5. Common Mistakes

- **Mistake:** Thinking `@EntityGraph` itself defines the query.
  - `@EntityGraph` controls fetching; the repository method still needs a query definition.

- **Mistake:** Thinking `@EntityGraph` always requires `@Query`.
  - It does not. The query can come from an inherited repository method or a valid derived-query method.

- **Mistake:** Creating an arbitrary custom method such as `findAllWithMonitorsUsingGraph()` without `@Query`.
  - Spring Data attempted to derive a query and searched for a `Workspace` property with that name.

- **Mistake:** Assuming `JOIN FETCH` always breaks pagination.
  - The precise issue is collection fetch joins combined with pagination; Hibernate may fall back to in-memory pagination.

- **Mistake:** Assuming `Hibernate.initialize()` eliminates N+1.
  - Explicit initialization can still issue one query per parent.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

N+1 means **1 parent query + N child queries**. It commonly happens when a lazy relationship is accessed for every parent in a list. `JOIN FETCH` and `@EntityGraph` can fetch the required relationship more efficiently. `@EntityGraph` controls the fetch plan; it does not itself define the query.

**Q&A**

**Q1. What is the N+1 problem?**
One query loads the parent entities and N additional queries load their related entities or collections.

**Q2. Why did Roost produce 4 queries for 3 workspaces?**
One query loaded the 3 workspaces and each workspace triggered one lazy monitor query: `1 + 3 = 4`.

**Q3. What did `JOIN FETCH` do?**
It explicitly told JPQL to fetch the related monitors as part of the query.

**Q4. What does `@EntityGraph(attributePaths = "monitors")` mean?**
It tells JPA that the `monitors` relationship should be fetched for that repository operation.

**Q5. Does `@EntityGraph` require `@Query`?**
No. It can be used with an inherited repository method or a valid derived query. Our custom method needed `@Query` because its name was not a valid derived query.

**Q6. Why did `findAllWithMonitorsUsingGraph()` fail without `@Query`?**
Spring Data tried to interpret it as a derived query and could not find a `Workspace` property named `findAllWithMonitorsUsingGraph`.

**Q7. Does `Hibernate.initialize()` solve N+1?**
Not necessarily. Initializing each parent's collection individually can still result in N additional queries.

**Q8. What should you check when using collection `JOIN FETCH` with pagination?**
Hibernate may perform pagination in memory rather than in SQL, which can be expensive for large result sets.

**Q9. How can N+1 be detected automatically in tests?**
Use an SQL query counter, such as Hypersistence Utils' `SQLStatementCountValidator`, and assert an expected maximum query count.

**Q10. Why is N+1 particularly dangerous for list endpoints?**
Because N grows with the number of parents returned, causing the number of database round trips to grow with the result size.

**Key Takeaway:**
**N+1 = 1 parent query + N child queries. `@EntityGraph` is a fetch-plan tool: use it when a particular query needs related data and you want to fetch that relationship deliberately instead of making the relationship globally EAGER.**
