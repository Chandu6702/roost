# Spring Data JPA Repositories

**Week 12 · Day 2** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

Spring Data JPA provides repository abstractions that remove most boilerplate database access code. Instead of writing an implementation for every CRUD operation, Roost extends `JpaRepository` and lets Spring Data generate the repository implementation. It also supports derived queries, where method names describe the query conditions, and `Pageable` for database-level pagination.

Day 2 completed the transition from the old in-memory monitor storage to PostgreSQL-backed repositories.

## 2. Mental Model

The basic flow is:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Spring Data JPA
    ↓
Hibernate
    ↓
PostgreSQL
```

For a derived query:

```text
findByWorkspaceIdAndNameContainingIgnoreCase(...)
                    ↓
          Spring Data parses method
                    ↓
             Builds query
                    ↓
                Hibernate
                    ↓
               PostgreSQL
```

For pagination:

```text
GET /monitors?page=0&size=5
              ↓
           Pageable
              ↓
     repository.findAll(pageable)
              ↓
        Page<Monitor>
              ↓
     Page<MonitorResponse>
```

The important difference from the Week 11 in-memory implementation is that PostgreSQL performs the pagination rather than Java loading every record first.

## 3. My Roost Code

### `WorkspaceRepository.java`

```java
public interface WorkspaceRepository
        extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlug(String slug);
}
```

### `MonitorRepository.java`

```java
public interface MonitorRepository
        extends JpaRepository<Monitor, Long> {

    List<Monitor> findByWorkspaceId(Long workspaceId);

    List<Monitor> findByWorkspaceIdAndNameContainingIgnoreCase(
            Long workspaceId,
            String q
    );
}
```

These methods are derived queries. Spring Data parses the method names and creates the appropriate query.

### Paginated repository access

`JpaRepository` already provides:

```java
Page<Monitor> findAll(Pageable pageable);
```

The service exposes it:

```java
public Page<Monitor> getMonitors(Pageable pageable) {
    return monitorRepository.findAll(pageable);
}
```

### Controller pagination

```java
@GetMapping
public ResponseEntity<Page<MonitorResponse>> getMonitors(
        Pageable pageable) {

    Page<MonitorResponse> responses =
            monitorService.getMonitors(pageable)
                    .map(this::toResponse);

    return ResponseEntity.ok(responses);
}
```

The endpoint was tested successfully with:

```text
GET /monitors?page=0&size=5
```

and returned `Page` metadata including `totalElements`, `totalPages`, `number`, and `size`.

## 4. Interview Points

- **What is `JpaRepository`?**
  A Spring Data repository abstraction providing CRUD operations plus JPA-specific functionality such as pagination, sorting, flushing, and batch operations.

- **How does derived query parsing work?**
  Spring Data parses keywords such as `And`, `Containing`, `IgnoreCase`, and entity property names from the repository method name.

- **What happens if a derived query method is invalid?**
  Spring Data generally detects the invalid property/query definition during application startup and fails to create the repository.

- **Why use a derived query instead of `@Query`?**
  Derived queries are concise and readable for straightforward conditions. `@Query` is more appropriate when the query becomes too complex for a method name.

- **JPQL vs native SQL?**
  JPQL operates on entities and their properties. Native SQL is useful when PostgreSQL-specific SQL features are required.

- **What does `JpaRepository` provide beyond basic CRUD?**
  Pagination, sorting, flushing, batch-related operations, and JPA-specific repository functionality.

- **What does `Page<Monitor>` contain?**
  The current page's content plus pagination metadata such as total elements, total pages, current page number, and page size.

- **Why is DB-level pagination preferable?**
  The database can return only the requested rows instead of the application loading the complete result set into memory.

## 5. Common Mistakes

- Writing a repository method name that doesn't match an entity property.
- Assuming Spring Data repository methods execute arbitrary Java logic.
- Using a native SQL query when a simple derived query is sufficient.
- Loading the complete database result and manually slicing it in Java.
- Forgetting that `Pageable` is zero-based: `page=0` is the first page.
- Confusing `Page<T>` with `List<T>` — `Page` includes metadata.
- Returning JPA entities directly when the API should expose DTOs.
- Forgetting to replace old in-memory repository/service logic after introducing persistence.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

> `JpaRepository` gives Roost CRUD, sorting, and pagination without manually implementing repository classes. Spring Data can derive queries from method names. `Pageable` allows the database query itself to return only the requested page.

**Q&A**

**Q: What happens when Spring sees `findBySlug`?**
A: It identifies `slug` as an entity property and creates an equality query for it.

**Q: What does `Containing` mean?**
A: It represents a substring/contains condition, typically translated to a `LIKE`-style query.

**Q: What does `IgnoreCase` do?**
A: It makes the string comparison case-insensitive.

**Q: Break down `findByWorkspaceIdAndNameContainingIgnoreCase`.**
A: Find monitors where `workspaceId` matches and `name` contains the supplied text without case sensitivity.

**Q: What if the repository method says `findByWorkspceId`?**
A: Spring Data cannot resolve that property and repository creation fails during startup.

**Q: Why does `findByWorkspaceId` work even though `Monitor` contains a `Workspace`, not a `workspaceId` field?**
A: Spring Data can traverse the entity property path: `workspace.id`.

**Q: What is the difference between `List<Monitor>` and `Page<Monitor>`?**
A: `List` contains only results. `Page` contains results plus pagination metadata.

**Q: What does `Pageable` contain?**
A: Page number, page size, and optional sorting information.

**Q: Is `page=0` the first page?**
A: Yes. Spring Data pagination is zero-based.

**Q: Does `JpaRepository.findAll(pageable)` load every monitor and then slice it?**
A: No. Spring Data passes pagination information through Hibernate to the database query.

**Q: When should you use `@Query`?**
A: When a query is too complex or specific to express cleanly through a derived method name.

**Q: When would native SQL be justified?**
A: When PostgreSQL-specific features or SQL behavior are required that JPQL cannot express conveniently.

**Q: What did Day 2 replace from Week 11?**
A: The in-memory `Map<Long, Monitor>` storage was replaced by a PostgreSQL-backed `MonitorRepository`.

**Q: Why keep `MonitorResponse` instead of returning `Monitor` directly?**
A: DTOs keep the API representation separate from the persistence model and prevent exposing the entity structure directly.

**Q: What does `findByWorkspaceId(Long workspaceId)` conceptually query?**
A: Monitors whose associated workspace has the supplied ID.

**Q: What does `findByWorkspaceIdAndNameContainingIgnoreCase(...)` add?**
A: It combines workspace filtering with case-insensitive partial name searching.

**Q: What did we verify manually?**
A: Workspace lookup, monitor lookup by workspace, case-insensitive monitor search, monitor persistence, and paginated monitor retrieval.

**Key Takeaway:**
**Spring Data JPA turns repository interfaces and method names into database access code, while `Pageable` lets PostgreSQL handle pagination instead of Java loading and slicing the entire dataset.**
